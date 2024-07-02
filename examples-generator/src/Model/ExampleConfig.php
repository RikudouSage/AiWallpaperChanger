<?php

namespace Rikudou\AiWallpaperChanger\ExamplesGenerator\Model;

use Rikudou\AiWallpaperChanger\ExamplesGenerator\Attribute\ArrayType;

final readonly class ExampleConfig
{
    /**
     * @param array<string> $models
     * @param array<string, array<string>>|null $params
     * @param array<CustomParameter>|null $customParameters
     */
    public function __construct(
        public string $name,
        public string $prompt,
        public array $models,
        public ?string $negativePrompt = null,
        public ?bool $hiresFix = null,
        public ?array $params = null,
        public ?string $author = null,
        public ?string $description = null,
        #[ArrayType(CustomParameter::class)]
        public ?array $customParameters = null,
    ) {
    }
}
