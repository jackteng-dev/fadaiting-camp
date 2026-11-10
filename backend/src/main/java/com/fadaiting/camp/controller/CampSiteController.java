package com.fadaiting.camp.controller;

import com.fadaiting.camp.dto.CampSiteDetailDto;
import com.fadaiting.camp.dto.CampZoneDetailDto;
import com.fadaiting.camp.pojo.CustomUserDetails;
import com.fadaiting.camp.pojo.Announcement;
import com.fadaiting.camp.pojo.Availability;
import com.fadaiting.camp.pojo.CampSite;
import com.fadaiting.camp.pojo.CampUnit;
import com.fadaiting.camp.pojo.CampZone;
import com.fadaiting.camp.pojo.Faq;
import com.fadaiting.camp.pojo.RatePlan;
import com.fadaiting.camp.pojo.RentalItem;
import com.fadaiting.camp.pojo.Result;
import com.fadaiting.camp.pojo.SiteImage;
import com.fadaiting.camp.service.CampSiteService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/camp")
public class CampSiteController {
    @Autowired
    private CampSiteService campSiteService;

    @GetMapping("/site")
    public Result<CampSite> site() {
        return Result.success(campSiteService.getActiveSite());
    }

    @GetMapping("/site/detail")
    public Result<CampSiteDetailDto> siteDetail() {
        return Result.success(campSiteService.getActiveSiteDetail());
    }

    @GetMapping("/zone")
    public Result<List<CampZoneDetailDto>> zones() {
        return Result.success(campSiteService.getActiveZoneDetails());
    }

    @GetMapping("/zone/list")
    public Result<List<CampZone>> zoneList() {
        return Result.success(campSiteService.getAdminZones());
    }

    @GetMapping("/unit/list")
    public Result<List<CampUnit>> unitList() {
        return Result.success(campSiteService.getAdminUnits());
    }

    @GetMapping("/rate-plan/list")
    public Result<List<RatePlan>> ratePlanList() {
        return Result.success(campSiteService.getAdminRatePlans());
    }

    @GetMapping("/availability/list")
    public Result<List<Availability>> availabilityList(
            @RequestParam(required = false) Integer campUnitId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return Result.success(campSiteService.getAdminAvailability(campUnitId, startDate, endDate));
    }

    @GetMapping("/rental-item/list")
    public Result<List<RentalItem>> rentalItemList() {
        return Result.success(campSiteService.getAdminRentalItems());
    }

    @GetMapping("/announcement")
    public Result<List<Announcement>> announcements() {
        return Result.success(campSiteService.getAdminAnnouncements());
    }

    @GetMapping("/faq")
    public Result<List<Faq>> faqs() {
        return Result.success(campSiteService.getAdminFaqs());
    }

    @GetMapping("/gallery")
    public Result<List<SiteImage>> gallery() {
        return Result.success(campSiteService.getAdminSiteImages());
    }

    @PutMapping("/site")
    public Result<?> updateSite(@RequestBody CampSite campSite) {
        campSiteService.updateActiveSite(campSite);
        return Result.success();
    }

    @PostMapping("/zone")
    public Result<?> createZone(@RequestBody CampZone campZone) {
        campSiteService.createZone(campZone);
        return Result.success();
    }

    @PutMapping("/zone")
    public Result<?> updateZone(@RequestBody CampZone campZone) {
        campSiteService.updateZone(campZone);
        return Result.success();
    }

    @DeleteMapping("/zone/{id}")
    public Result<?> deleteZone(@PathVariable Integer id) {
        campSiteService.deleteZone(id);
        return Result.success();
    }

    @PostMapping("/unit")
    public Result<?> createUnit(@RequestBody CampUnit campUnit) {
        campSiteService.createUnit(campUnit);
        return Result.success();
    }

    @PutMapping("/unit")
    public Result<?> updateUnit(@RequestBody CampUnit campUnit) {
        campSiteService.updateUnit(campUnit);
        return Result.success();
    }

    @DeleteMapping("/unit/{id}")
    public Result<?> deleteUnit(@PathVariable Integer id) {
        campSiteService.deleteUnit(id);
        return Result.success();
    }

    @PostMapping("/rate-plan")
    public Result<?> createRatePlan(@RequestBody RatePlan ratePlan) {
        campSiteService.createRatePlan(ratePlan);
        return Result.success();
    }

    @PutMapping("/rate-plan")
    public Result<?> updateRatePlan(@RequestBody RatePlan ratePlan) {
        campSiteService.updateRatePlan(ratePlan);
        return Result.success();
    }

    @DeleteMapping("/rate-plan/{id}")
    public Result<?> deleteRatePlan(@PathVariable Integer id) {
        campSiteService.deleteRatePlan(id);
        return Result.success();
    }

    @PostMapping("/availability")
    public Result<?> createAvailability(@RequestBody Availability availability) {
        campSiteService.createAvailability(availability);
        return Result.success();
    }

    @PutMapping("/availability")
    public Result<?> updateAvailability(@RequestBody Availability availability) {
        campSiteService.updateAvailability(availability);
        return Result.success();
    }

    @DeleteMapping("/availability/{id}")
    public Result<?> deleteAvailability(@PathVariable Integer id) {
        campSiteService.deleteAvailability(id);
        return Result.success();
    }

    @PostMapping("/rental-item")
    public Result<?> createRentalItem(@RequestBody RentalItem rentalItem) {
        campSiteService.createRentalItem(rentalItem);
        return Result.success();
    }

    @PutMapping("/rental-item")
    public Result<?> updateRentalItem(@RequestBody RentalItem rentalItem) {
        campSiteService.updateRentalItem(rentalItem);
        return Result.success();
    }

    @DeleteMapping("/rental-item/{id}")
    public Result<?> deleteRentalItem(@PathVariable Integer id) {
        campSiteService.deleteRentalItem(id);
        return Result.success();
    }

    @PostMapping("/announcement")
    public Result<?> createAnnouncement(
            @RequestBody Announcement announcement,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        campSiteService.createAnnouncement(announcement, currentUser.getId());
        return Result.success();
    }

    @PutMapping("/announcement")
    public Result<?> updateAnnouncement(@RequestBody Announcement announcement) {
        campSiteService.updateAnnouncement(announcement);
        return Result.success();
    }

    @DeleteMapping("/announcement/{id}")
    public Result<?> deleteAnnouncement(@PathVariable Integer id) {
        campSiteService.deleteAnnouncement(id);
        return Result.success();
    }

    @PostMapping("/faq")
    public Result<?> createFaq(@RequestBody Faq faq) {
        campSiteService.createFaq(faq);
        return Result.success();
    }

    @PutMapping("/faq")
    public Result<?> updateFaq(@RequestBody Faq faq) {
        campSiteService.updateFaq(faq);
        return Result.success();
    }

    @DeleteMapping("/faq/{id}")
    public Result<?> deleteFaq(@PathVariable Integer id) {
        campSiteService.deleteFaq(id);
        return Result.success();
    }

    @PostMapping("/gallery")
    public Result<?> createSiteImage(@RequestBody SiteImage siteImage) {
        campSiteService.createSiteImage(siteImage);
        return Result.success();
    }

    @PutMapping("/gallery")
    public Result<?> updateSiteImage(@RequestBody SiteImage siteImage) {
        campSiteService.updateSiteImage(siteImage);
        return Result.success();
    }

    @DeleteMapping("/gallery/{id}")
    public Result<?> deleteSiteImage(@PathVariable Integer id) {
        campSiteService.deleteSiteImage(id);
        return Result.success();
    }
}
