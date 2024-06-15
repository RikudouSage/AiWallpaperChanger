<?php

namespace Rikudou\AiWallpaperChanger\ExamplesGenerator\Model;

final readonly class ExampleConfig
{
    /**
     * @param array<string> $models
     */
    public function __construct(
        public string $name,
        public string $prompt,
        public array $models,
        public ?string $negativePrompt = null,
        public ?bool $hiresFix = null,
    ) {
    }
}
