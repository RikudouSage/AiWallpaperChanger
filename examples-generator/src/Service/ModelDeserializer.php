<?php

namespace Rikudou\AiWallpaperChanger\ExamplesGenerator\Service;

use ReflectionClass;
use ReflectionNamedType;
use ReflectionType;
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
            $params[$key] = $this->parseParam($value, $classProperty->getType());
        }

        return new $class(...$params);
    }

    private function parseParam(mixed $value, ?ReflectionType $type): mixed
    {
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
            return (array) $value;
        }
        if ($typeName === 'object') {
            return (object) $value;
        }
        if (class_exists($typeName)) {
            return $this->deserialize($value, $type->getName());
        }

        throw new RuntimeException('Unsupported type: ' . $type->getName());
    }
}
