package ru.practicum.eventservice.category.mapper;

import lombok.experimental.UtilityClass;
import ru.practicum.eventservice.category.dto.CategoryDto;
import ru.practicum.eventservice.category.dto.CategoryDtoOut;
import ru.practicum.eventservice.category.model.Category;

@UtilityClass
public class CategoryMapper {

    public static CategoryDtoOut toDto(Category category) {
        return new CategoryDtoOut(category.getId(), category.getName());
    }

    public static Category fromDto(CategoryDto dto) {
        return new Category(null, dto.getName());
    }
}
