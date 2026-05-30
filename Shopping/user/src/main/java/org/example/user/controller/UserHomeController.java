package org.example.user.controller;

import org.example.common.Result;
import org.example.user.service.UserHomeService;
import org.example.user.vo.HomeBannerVO;
import org.example.user.vo.HomePageVO;
import org.example.user.vo.HomeSectionVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/user/home")
public class UserHomeController {
    private final UserHomeService userHomeService;

    public UserHomeController(UserHomeService userHomeService) {
        this.userHomeService = userHomeService;
    }

    @GetMapping
    public Result<HomePageVO> home() {
        return Result.ok(userHomeService.getHomePage());
    }

    @GetMapping("/banners")
    public Result<List<HomeBannerVO>> banners() {
        return Result.ok(userHomeService.listBanners());
    }

    @GetMapping("/sections")
    public Result<List<HomeSectionVO>> sections() {
        return Result.ok(userHomeService.listSections());
    }
}
