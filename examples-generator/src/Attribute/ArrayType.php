<?php

namespace Rikudou\AiWallpaperChanger\ExamplesGenerator\Attribute;

use Attribute;

#[Attribute(Attribute::TARGET_PROPERTY)]
final readonly class ArrayType
{
    /**
     * @param class-string $class
     */
    public function __construct(
        public string $class,
    ) {
    }
}
