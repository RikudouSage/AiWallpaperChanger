<?php

use Aws\S3\S3Client;
use Rikudou\AiWallpaperChanger\ExamplesGenerator\Model\ExampleConfig;
use Rikudou\AiWallpaperChanger\ExamplesGenerator\Service\ModelDeserializer;

require_once __DIR__ . '/vendor/autoload.php';

$file = __DIR__ . '/../app/src/main/res/raw/premade_prompts.json';
$deserializer = new ModelDeserializer();
$configs = array_map(
    fn (array $config) => $deserializer->deserialize($config, ExampleConfig::class),
    json_decode(file_get_contents($file), true),
);
$s3client = new S3Client([
    'version' => 'latest',
    'region' => 'auto',
    'endpoint' => getenv('R2_ENDPOINT'),
]);
$bucket = getenv('R2_BUCKET_NAME') ?: throw new RuntimeException('No bucket specified');

foreach ($configs as $config) {
    echo "Creating new hash for {$config->name}", PHP_EOL;
    $hash = hash('sha256', serialize($config));
    $s3client->putObject([
        'Bucket' => $bucket,
        'Key' => "{$config->name}/hash.json",
        'Body' => $hash,
        'ContentType' => 'application/json',
    ]);
}
