package ru.practicum.category.mapper;

import lombok.experimental.UtilityClass;
import ru.practicum.category.dto.CategoryDto;
import ru.practicum.category.dto.CategoryDtoOut;
import ru.practicum.category.model.Category;

@UtilityClass
public class CategoryMapper {

    public static CategoryDtoOut toDto(Category category) {
        return new CategoryDtoOut(category.getId(), category.getName());
    }

    public static Category fromDto(CategoryDto dto) {
        return new Category(null, dto.getName());
    }
}
