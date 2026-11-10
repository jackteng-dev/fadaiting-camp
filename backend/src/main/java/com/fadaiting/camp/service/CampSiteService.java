package com.fadaiting.camp.service;

import com.fadaiting.camp.dto.CampSiteDetailDto;
import com.fadaiting.camp.dto.CampZoneDetailDto;
import com.fadaiting.camp.pojo.Announcement;
import com.fadaiting.camp.pojo.Availability;
import com.fadaiting.camp.pojo.CampSite;
import com.fadaiting.camp.pojo.CampUnit;
import com.fadaiting.camp.pojo.CampZone;
import com.fadaiting.camp.pojo.Faq;
import com.fadaiting.camp.pojo.RatePlan;
import com.fadaiting.camp.pojo.RentalItem;
import com.fadaiting.camp.pojo.SiteImage;

import java.time.LocalDate;
import java.util.List;

public interface CampSiteService {
    CampSite getActiveSite();

    CampSiteDetailDto getActiveSiteDetail();

    List<CampZoneDetailDto> getActiveZoneDetails();

    List<CampZone> getAdminZones();

    List<CampUnit> getAdminUnits();

    List<RatePlan> getAdminRatePlans();

    List<Availability> getAdminAvailability(Integer campUnitId, LocalDate startDate, LocalDate endDate);

    List<RentalItem> getAdminRentalItems();

    List<RentalItem> getActiveRentalItems();

    List<Announcement> getAdminAnnouncements();

    List<Faq> getAdminFaqs();

    List<SiteImage> getAdminSiteImages();

    List<Announcement> getActiveAnnouncements();

    void updateActiveSite(CampSite campSite);

    void createZone(CampZone campZone);

    void updateZone(CampZone campZone);

    void deleteZone(Integer id);

    void createUnit(CampUnit campUnit);

    void updateUnit(CampUnit campUnit);

    void deleteUnit(Integer id);

    void createRatePlan(RatePlan ratePlan);

    void updateRatePlan(RatePlan ratePlan);

    void deleteRatePlan(Integer id);

    void createAvailability(Availability availability);

    void updateAvailability(Availability availability);

    void deleteAvailability(Integer id);

    void createRentalItem(RentalItem rentalItem);

    void updateRentalItem(RentalItem rentalItem);

    void deleteRentalItem(Integer id);

    void createAnnouncement(Announcement announcement, Integer currentUserId);

    void updateAnnouncement(Announcement announcement);

    void deleteAnnouncement(Integer id);

    void createFaq(Faq faq);

    void updateFaq(Faq faq);

    void deleteFaq(Integer id);

    void createSiteImage(SiteImage siteImage);

    void updateSiteImage(SiteImage siteImage);

    void deleteSiteImage(Integer id);
}
