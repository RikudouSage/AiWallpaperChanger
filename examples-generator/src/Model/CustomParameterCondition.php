<?php

namespace Rikudou\AiWallpaperChanger\ExamplesGenerator\Model;

final readonly class CustomParameterCondition
{
    public function __construct(
        public string $value,
        public CustomParameterConditionType $type,
        public ?string $expression = null,
    ) {
    }
}
