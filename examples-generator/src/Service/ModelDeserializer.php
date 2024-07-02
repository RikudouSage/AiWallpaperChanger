<?php

namespace Rikudou\AiWallpaperChanger\ExamplesGenerator\Service;

use BackedEnum;
use ReflectionClass;
use ReflectionNamedType;
use ReflectionProperty;
use Rikudou\AiWallpaperChanger\ExamplesGenerator\Attribute\ArrayType;
use RuntimeException;

/**
 * @template T of object
 */
final readonly class ModelDeserializer
{
    /**
     * @noinspection PhpDocSignatureInspection
     *
     * @param array<string, mixed> $raw
     * @param class-string<T> $class
     *
     * @return T
     */
    public function deserialize(array $raw, string $class): object
    {
        $reflection = new ReflectionClass($class);
        $params = [];
        foreach ($raw as $key => $value) {
            $classProperty = $reflection->getProperty($key);
            $params[$key] = $this->parseParam($value, $classProperty);
        }

        return new $class(...$params);
    }

    private function parseParam(mixed $value, ReflectionProperty $parameter): mixed
    {
        $type = $parameter->getType();

        if ($type === null) {
            return $value;
        }
        if (!$type instanceof ReflectionNamedType) {
            throw new RuntimeException('Type must be a single (or nullable) type');
        }

        if ($value === null && $type->allowsNull()) {
            return null;
        }

        $typeName = $type->getName();
        if ($typeName === 'string') {
            return (string) $value;
        }
        if ($typeName === 'int') {
            return (int) $value;
        }
        if ($typeName === 'float') {
            return (float) $value;
        }
        if ($typeName === 'bool') {
            return (bool) $value;
        }
        if ($typeName === 'array') {
            if ($attribute = $this->getAttribute($parameter, ArrayType::class)) {
                $result = [];

                foreach ($value as $item) {
                    $result[] = $this->deserialize($item, $attribute->class);
                }

                return $result;
            }
            return (array) $value;
        }
        if ($typeName === 'object') {
            return (object) $value;
        }
        if (is_a($typeName, BackedEnum::class, true)) {
            return $typeName::from($value);
        }
        if (class_exists($typeName)) {
            return $this->deserialize($value, $type->getName());
        }

        throw new RuntimeException('Unsupported type: ' . $type->getName());
    }

    /**
     * @template T of object
     *
     * @param class-string<T> $attribute
     *
     * @return T|null
     */
    private function getAttribute(ReflectionProperty $parameter, string $attribute): ?object
    {
        $attributes = $parameter->getAttributes($attribute);
        if (!count($attributes)) {
            return null;
        }

        return $attributes[array_key_first($attributes)]->newInstance();
    }
}
