package org.example.user.service;

import org.example.mapper.UserHomeMapper;
import org.example.mapper.UserProductMapper;
import org.example.user.dto.ProductQueryDTO;
import org.example.user.vo.CategoryVO;
import org.example.user.vo.HomeBannerVO;
import org.example.user.vo.HomePageVO;
import org.example.user.vo.HomeSectionVO;
import org.example.user.vo.ProductListItemVO;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class UserHomeService {
    private final UserHomeMapper userHomeMapper;
    private final UserProductMapper userProductMapper;
    private final UserProductService userProductService;

    public UserHomeService(UserHomeMapper userHomeMapper,
                           UserProductMapper userProductMapper,
                           UserProductService userProductService) {
        this.userHomeMapper = userHomeMapper;
        this.userProductMapper = userProductMapper;
        this.userProductService = userProductService;
    }

    public HomePageVO getHomePage() {
        List<HomeSectionVO> sections = listSections();
        HomePageVO page = new HomePageVO();
        page.setBanners(listBanners());
        page.setSections(sections);
        page.setCategories(listFeaturedCategories());
        page.setHotKeywords(userHomeMapper.selectHotKeywords());
        page.setFreshProducts(listFreshProducts(sections));
        return page;
    }

    public List<HomeBannerVO> listBanners() {
        return userHomeMapper.selectHomeBanners();
    }

    public List<HomeSectionVO> listSections() {
        List<HomeSectionVO> sections = userHomeMapper.selectHomeSections();
        for (HomeSectionVO section : sections) {
            section.setGoods(userHomeMapper.selectSectionGoods(section.getSectionId()));
        }
        return sections;
    }

    private List<CategoryVO> listFeaturedCategories() {
        return userProductMapper.selectEnabledCategories()
                .stream()
                .filter(category -> category.getParentCateId() == null || category.getParentCateId() == 0)
                .limit(10)
                .toList();
    }

    private List<ProductListItemVO> listFreshProducts(List<HomeSectionVO> sections) {
        ProductQueryDTO query = new ProductQueryDTO();
        query.setPageNum(1);
        query.setPageSize(12);
        query.setSort("newest");
        Set<Long> sectionGoodsIds = new HashSet<>();
        for (HomeSectionVO section : sections) {
            if (section.getGoods() == null) {
                continue;
            }
            for (ProductListItemVO goods : section.getGoods()) {
                if (goods.getGoodsId() != null) {
                    sectionGoodsIds.add(goods.getGoodsId());
                }
            }
        }
        return userProductService.listProducts(query)
                .records()
                .stream()
                .filter(item -> !sectionGoodsIds.contains(item.getGoodsId()))
                .limit(4)
                .toList();
    }
}
