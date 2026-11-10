package com.fadaiting.camp.controller;

import com.fadaiting.camp.dto.CampSiteDetailDto;
import com.fadaiting.camp.dto.CampZoneDetailDto;
import com.fadaiting.camp.pojo.Announcement;
import com.fadaiting.camp.pojo.RentalItem;
import com.fadaiting.camp.pojo.Result;
import com.fadaiting.camp.service.CampSiteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/public/camp")
public class PublicCampController {
    @Autowired
    private CampSiteService campSiteService;

    @GetMapping("/site")
    public Result<CampSiteDetailDto> site() {
        return Result.success(campSiteService.getActiveSiteDetail());
    }

    @GetMapping("/zones")
    public Result<List<CampZoneDetailDto>> zones() {
        return Result.success(campSiteService.getActiveZoneDetails());
    }

    @GetMapping("/announcements")
    public Result<List<Announcement>> announcements() {
        return Result.success(campSiteService.getActiveAnnouncements());
    }

    @GetMapping("/rental-items")
    public Result<List<RentalItem>> rentalItems() {
        return Result.success(campSiteService.getActiveRentalItems());
    }
}
