<?php

use Aws\S3\S3Client;
use GuzzleHttp\Client;
use Rikudou\AiWallpaperChanger\ExamplesGenerator\Model\ExampleConfig;
use Rikudou\AiWallpaperChanger\ExamplesGenerator\Service\ModelDeserializer;

require __DIR__ . '/vendor/autoload.php';

$validity = new DateInterval('P7D');
$maxImages = 4;

$deserializer = new ModelDeserializer();
$file = __DIR__ . '/../app/src/main/res/raw/premade_prompts.json';
$configs = array_map(
    fn (array $config) => $deserializer->deserialize($config, ExampleConfig::class),
    json_decode(file_get_contents($file), true),
);
$bucket = getenv('R2_BUCKET_NAME') ?: throw new RuntimeException('No bucket specified');

$s3client = new S3Client([
    'version' => 'latest',
    'region' => 'auto',
    'endpoint' => getenv('R2_ENDPOINT'),
]);
$httpClient = new Client();

/** @var array<string, array<string>> $resolved */
$resolved = [];
/** @var array<string, string> $toCheck */
$toCheck = [];
foreach ($configs as $config) {
    $hash = hash('sha256', serialize($config));
    $objects = $s3client->listObjectsV2([
        'Bucket' => $bucket,
        'Prefix' => $config->name,
    ])->get('Contents');

    $create = 0;
    if ($objects === null) {
        echo "[{$config->name}] No objects for style, creating a new hash", PHP_EOL;
        $create = $maxImages;
        $s3client->putObject([
            'Bucket' => $bucket,
            'Key' => "{$config->name}/hash.json",
            'Body' => $hash,
            'ContentType' => 'application/json',
        ]);
    } else {
        $existingHash = (string) $s3client->getObject([
            'Bucket' => $bucket,
            'Key' => "{$config->name}/hash.json",
        ])->get('Body');
        if ($existingHash !== $hash) {
            echo "[{$config->name}] The existing hash is not the same as the current one, deleting all images", PHP_EOL;
            foreach ($objects as $object) {
                $s3client->deleteObject([
                    'Bucket' => $bucket,
                    'Key' => "{$object['Key']}",
                ]);
            }
            $s3client->putObject([
                'Bucket' => $bucket,
                'Key' => "{$config->name}/hash.json",
                'Body' => $hash,
                'ContentType' => 'application/json',
            ]);
            $create = $maxImages;
        } else {
            $create = $maxImages - count($objects) + 1;
            foreach ($objects as $object) {
                if ($object['Key'] === "{$config->name}/hash.json") {
                    continue;
                }
                if ($object['LastModified']->add($validity) < new DateTimeImmutable()) {
                    echo "[{$config->name}] Found an old file, deleting", PHP_EOL;
                    $s3client->deleteObject([
                        'Bucket' => $bucket,
                        'Key' => "{$config->name}/{$object['Key']}",
                    ]);
                    ++$create;
                }
            }
        }
    }

    if (!$create) {
        echo "[{$config->name}] No new objects needed, skipping", PHP_EOL;
        continue;
    }

    echo "[{$config->name}] Sending a request for {$create} images", PHP_EOL;
    $response = $httpClient->post('https://aihorde.net/api/v2/generate/async', [
        'json' => [
            'prompt' => $config->negativePrompt ? ("{$config->prompt} ### {$config->negativePrompt}") : $config->prompt,
            'params' => [
                'hires_fix' => $config->hiresFix ?? false,
                'n' => $create,
                'width' => 512,
                'height' => 1024,
            ],
            'models' => $config->models,
            'slow_workers' => false,
        ],
        'headers' => [
            'apikey' => getenv('AI_HORDE_API_KEY'),
        ],
    ]);
    $body = json_decode($response->getBody(), true);
    $toCheck[$config->name] = $body['id'];
}

echo "===========================================", PHP_EOL;

while (count($resolved) !== count($toCheck)) {
    foreach ($toCheck as $name => $id) {
        if (isset($resolved[$name])) {
            continue;
        }
        $response = $httpClient->get("https://aihorde.net/api/v2/generate/check/{$id}", [
            'headers' => [
                'apikey' => getenv('AI_HORDE_API_KEY'),
            ],
        ]);
        $body = json_decode($response->getBody(), true);
        if (!$body['is_possible']) {
            echo "[{$name}] Finishing a request is not possible", PHP_EOL;
            $resolved[$name] = [];
            continue;
        }
        if (!$body['done']) {
            echo "[{$name}] Not done yet", PHP_EOL;
            continue;
        }

        $response = $httpClient->get("https://aihorde.net/api/v2/generate/status/{$id}", [
            'headers' => [
                'apikey' => getenv('AI_HORDE_API_KEY'),
            ],
        ]);
        $body = json_decode($response->getBody(), true);
        $resolved[$name] = array_map(
            fn (array $item) => $item['img'],
            array_filter(
                $body['generations'],
                fn (array $item) => !$item['censored'] && $item['img'],
            )
        );
        echo "[{$name}] Got new images, storing them in R2", PHP_EOL;
        foreach ($resolved[$name] as $url) {
            $random = random_int(0, PHP_INT_MAX);
            $s3client->putObject([
                'Bucket' => $bucket,
                'Key' => "{$name}/{$random}.webp",
                'Body' => $httpClient->get($url)->getBody(),
                'ContentType' => 'image/webp',
            ]);
        }
    }
    echo "===========================================", PHP_EOL;
    sleep(2);
}

echo "All done", PHP_EOL;
