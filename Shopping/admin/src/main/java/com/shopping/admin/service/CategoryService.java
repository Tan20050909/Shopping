package com.shopping.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shopping.admin.entity.Category;
import com.shopping.admin.mapper.CategoryMapper;
import com.shopping.admin.mapper.GoodsMapper;
import com.shopping.admin.entity.Goods;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService extends ServiceImpl<CategoryMapper, Category> {

    private final GoodsMapper goodsMapper;

    public List<Category> listAll() {
        return list(new LambdaQueryWrapper<Category>().orderByAsc(Category::getSortNo));
    }

    public long countGoodsByCategoryId(Long categoryId) {
        return goodsMapper.selectCount(new LambdaQueryWrapper<Goods>().eq(Goods::getCategoryId, categoryId));
    }
}
