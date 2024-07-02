<?php

namespace Rikudou\AiWallpaperChanger\ExamplesGenerator\Model;

use Rikudou\AiWallpaperChanger\ExamplesGenerator\Attribute\ArrayType;

final readonly class CustomParameter
{
    /**
     * @param array<CustomParameterCondition> $conditions
     */
    public function __construct(
        public string $name,
        public string $expression,
        #[ArrayType(CustomParameterCondition::class)]
        public array $conditions,
        public ?string $description = null,
    ) {
    }
}
