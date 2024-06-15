<?php

namespace Rikudou\AiWallpaperChanger\ExamplesGenerator\Model;

final readonly class ExampleConfig
{
    /**
     * @param array<string> $models
     * @param array<string, array<string>>|null $params
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
    ) {
    }
}
