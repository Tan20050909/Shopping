package com.shopping.admin.controller;

import com.shopping.admin.common.Result;
import com.shopping.admin.entity.Category;
import com.shopping.admin.service.CategoryService;
import com.shopping.admin.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/category")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping("/list")
    public Result<List<Category>> list() {
        return Result.success(categoryService.listAll());
    }

    @GetMapping("/{id}")
    public Result<Category> detail(@PathVariable Long id) {
        return Result.success(categoryService.getById(id));
    }

    @PostMapping
    public Result<Void> add(@RequestBody Category category) {
        // 校验同级分类名唯一
        long count = categoryService.count(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Category>()
                .eq(Category::getCategoryName, category.getCategoryName())
                .eq(Category::getParentId, category.getParentId() != null ? category.getParentId() : 0));
        if (count > 0) {
            throw new BusinessException("同级下已存在相同名称的分类");
        }
        categoryService.save(category);
        return Result.success();
    }

    @PutMapping
    public Result<Void> update(@RequestBody Category category) {
        categoryService.updateById(category);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        // 检查是否有子分类
        long childCount = categoryService.count(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Category>()
                .eq(Category::getParentId, id));
        if (childCount > 0) {
            throw new BusinessException("该分类下存在子分类，无法删除");
        }
        // 检查是否有关联商品
        long goodsCount = categoryService.countGoodsByCategoryId(id);
        if (goodsCount > 0) {
            throw new BusinessException("该分类下存在商品，无法删除");
        }
        categoryService.removeById(id);
        return Result.success();
    }
}
