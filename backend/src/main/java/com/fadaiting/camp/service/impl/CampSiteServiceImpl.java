package com.fadaiting.camp.service.impl;

import com.fadaiting.camp.dto.CampSiteDetailDto;
import com.fadaiting.camp.dto.CampZoneDetailDto;
import com.fadaiting.camp.mapper.AnnouncementMapper;
import com.fadaiting.camp.mapper.AvailabilityMapper;
import com.fadaiting.camp.mapper.CampSiteMapper;
import com.fadaiting.camp.mapper.CampUnitMapper;
import com.fadaiting.camp.mapper.CampZoneMapper;
import com.fadaiting.camp.mapper.FaqMapper;
import com.fadaiting.camp.mapper.RatePlanMapper;
import com.fadaiting.camp.mapper.RentalItemMapper;
import com.fadaiting.camp.mapper.SiteImageMapper;
import com.fadaiting.camp.pojo.Announcement;
import com.fadaiting.camp.pojo.Availability;
import com.fadaiting.camp.pojo.CampSite;
import com.fadaiting.camp.pojo.CampUnit;
import com.fadaiting.camp.pojo.CampZone;
import com.fadaiting.camp.pojo.Faq;
import com.fadaiting.camp.pojo.RatePlan;
import com.fadaiting.camp.pojo.RentalItem;
import com.fadaiting.camp.pojo.SiteImage;
import com.fadaiting.camp.service.CampSiteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class CampSiteServiceImpl implements CampSiteService {
    @Autowired
    private CampSiteMapper campSiteMapper;
    @Autowired
    private CampZoneMapper campZoneMapper;
    @Autowired
    private CampUnitMapper campUnitMapper;
    @Autowired
    private AnnouncementMapper announcementMapper;
    @Autowired
    private FaqMapper faqMapper;
    @Autowired
    private SiteImageMapper siteImageMapper;
    @Autowired
    private RatePlanMapper ratePlanMapper;
    @Autowired
    private AvailabilityMapper availabilityMapper;
    @Autowired
    private RentalItemMapper rentalItemMapper;

    @Override
    public CampSite getActiveSite() {
        return campSiteMapper.findActiveSite();
    }

    @Override
    public CampSiteDetailDto getActiveSiteDetail() {
        CampSite site = getActiveSite();
        if (site == null) {
            return null;
        }

        CampSiteDetailDto detailDto = new CampSiteDetailDto();
        detailDto.setSite(site);
        detailDto.setFacilities(campSiteMapper.findFacilitiesBySiteId(site.getId()));
        detailDto.setFaqs(campSiteMapper.findFaqsBySiteId(site.getId()));
        detailDto.setImages(campSiteMapper.findImagesBySiteId(site.getId()));
        return detailDto;
    }

    @Override
    public List<CampZoneDetailDto> getActiveZoneDetails() {
        CampSite site = getActiveSite();
        if (site == null) {
            return Collections.emptyList();
        }

        List<CampZone> zones = campZoneMapper.findZonesBySiteId(site.getId());
        List<CampZoneDetailDto> details = new ArrayList<>();
        for (CampZone zone : zones) {
            CampZoneDetailDto detailDto = new CampZoneDetailDto();
            detailDto.setZone(zone);
            detailDto.setUnits(campZoneMapper.findUnitsByZoneId(zone.getId()));
            details.add(detailDto);
        }
        return details;
    }

    @Override
    public List<CampZone> getAdminZones() {
        CampSite site = getActiveSite();
        if (site == null) {
            return Collections.emptyList();
        }
        return campZoneMapper.findAdminZonesBySiteId(site.getId());
    }

    @Override
    public List<CampUnit> getAdminUnits() {
        CampSite site = getActiveSite();
        if (site == null) {
            return Collections.emptyList();
        }
        return campUnitMapper.findAdminUnitsBySiteId(site.getId());
    }

    @Override
    public List<RatePlan> getAdminRatePlans() {
        CampSite site = getActiveSite();
        if (site == null) {
            return Collections.emptyList();
        }
        return ratePlanMapper.findAdminRatePlansBySiteId(site.getId());
    }

    @Override
    public List<Availability> getAdminAvailability(Integer campUnitId, LocalDate startDate, LocalDate endDate) {
        CampSite site = getActiveSite();
        if (site == null) {
            return Collections.emptyList();
        }
        if (campUnitId != null) {
            ensureUnitBelongsToSite(campUnitId, site.getId());
        }
        return availabilityMapper.findAdminAvailability(site.getId(), campUnitId, startDate, endDate);
    }

    @Override
    public List<RentalItem> getAdminRentalItems() {
        CampSite site = getActiveSite();
        if (site == null) {
            return Collections.emptyList();
        }
        return rentalItemMapper.findAdminRentalItemsBySiteId(site.getId());
    }

    @Override
    public List<RentalItem> getActiveRentalItems() {
        CampSite site = getActiveSite();
        if (site == null) {
            return Collections.emptyList();
        }
        return rentalItemMapper.findActiveRentalItemsBySiteId(site.getId());
    }

    @Override
    public List<Announcement> getAdminAnnouncements() {
        CampSite site = getActiveSite();
        if (site == null) {
            return Collections.emptyList();
        }
        return announcementMapper.findAdminAnnouncementsBySiteId(site.getId());
    }

    @Override
    public List<Faq> getAdminFaqs() {
        CampSite site = getActiveSite();
        if (site == null) {
            return Collections.emptyList();
        }
        return faqMapper.findAdminFaqsBySiteId(site.getId());
    }

    @Override
    public List<SiteImage> getAdminSiteImages() {
        CampSite site = getActiveSite();
        if (site == null) {
            return Collections.emptyList();
        }
        return siteImageMapper.findAdminImagesBySiteId(site.getId());
    }

    @Override
    public List<Announcement> getActiveAnnouncements() {
        CampSite site = getActiveSite();
        if (site == null) {
            return Collections.emptyList();
        }
        return announcementMapper.findActiveAnnouncementsBySiteId(site.getId());
    }

    @Override
    public void updateActiveSite(CampSite campSite) {
        CampSite existingSite = getActiveSite();
        if (existingSite == null) {
            return;
        }

        campSite.setId(existingSite.getId());
        if (campSite.getSlug() == null || campSite.getSlug().isBlank()) {
            campSite.setSlug(existingSite.getSlug());
        }
        campSiteMapper.updateSite(campSite);
    }

    @Override
    public void createZone(CampZone campZone) {
        CampSite site = getActiveSite();
        if (site == null) {
            throw new IllegalArgumentException("找不到營區資料");
        }

        normalizeZone(campZone);
        campZone.setCampSiteId(site.getId());
        ensureUniqueZoneSlug(site.getId(), campZone.getSlug(), null);
        campZoneMapper.insertZone(campZone);
    }

    @Override
    public void updateZone(CampZone campZone) {
        CampSite site = getActiveSite();
        if (site == null) {
            throw new IllegalArgumentException("找不到營區資料");
        }
        if (campZone.getId() == null) {
            throw new IllegalArgumentException("缺少區域 ID");
        }

        CampZone existingZone = campZoneMapper.findById(campZone.getId());
        if (existingZone == null || !site.getId().equals(existingZone.getCampSiteId())) {
            throw new IllegalArgumentException("找不到營區區域");
        }

        normalizeZone(campZone);
        campZone.setCampSiteId(site.getId());
        ensureUniqueZoneSlug(site.getId(), campZone.getSlug(), campZone.getId());
        campZoneMapper.updateZone(campZone);
    }

    @Override
    public void deleteZone(Integer id) {
        CampSite site = getActiveSite();
        if (site == null) {
            throw new IllegalArgumentException("找不到營區資料");
        }

        CampZone existingZone = campZoneMapper.findById(id);
        if (existingZone == null || !site.getId().equals(existingZone.getCampSiteId())) {
            throw new IllegalArgumentException("找不到營區區域");
        }

        Integer unitCount = campZoneMapper.countUnitsByZoneId(id);
        if (unitCount != null && unitCount > 0) {
            throw new IllegalArgumentException("此區域已有營位，請先調整營位後再刪除");
        }
        campZoneMapper.deleteZone(id, site.getId());
    }

    private void normalizeZone(CampZone campZone) {
        if (campZone.getName() == null || campZone.getName().isBlank()) {
            throw new IllegalArgumentException("請填寫區域名稱");
        }
        if (campZone.getSlug() == null || campZone.getSlug().isBlank()) {
            throw new IllegalArgumentException("請填寫區域 slug");
        }
        if (campZone.getZoneType() == null || campZone.getZoneType().isBlank()) {
            campZone.setZoneType("tent");
        }
        if (campZone.getSortOrder() == null) {
            campZone.setSortOrder(0);
        }
        if (campZone.getIsActive() == null) {
            campZone.setIsActive(true);
        }
    }

    private void ensureUniqueZoneSlug(Integer campSiteId, String slug, Integer excludeId) {
        Integer count = campZoneMapper.countBySlug(campSiteId, slug, excludeId);
        if (count != null && count > 0) {
            throw new IllegalArgumentException("區域 slug 已存在");
        }
    }

    @Override
    public void createUnit(CampUnit campUnit) {
        CampSite site = getActiveSite();
        if (site == null) {
            throw new IllegalArgumentException("找不到營區資料");
        }

        normalizeUnit(campUnit);
        ensureZoneBelongsToSite(campUnit.getZoneId(), site.getId());
        campUnit.setCampSiteId(site.getId());
        campUnitMapper.insertUnit(campUnit);
    }

    @Override
    public void updateUnit(CampUnit campUnit) {
        CampSite site = getActiveSite();
        if (site == null) {
            throw new IllegalArgumentException("找不到營區資料");
        }
        if (campUnit.getId() == null) {
            throw new IllegalArgumentException("缺少營位 ID");
        }

        CampUnit existingUnit = campUnitMapper.findById(campUnit.getId());
        if (existingUnit == null || !site.getId().equals(existingUnit.getCampSiteId())) {
            throw new IllegalArgumentException("找不到營位資料");
        }

        normalizeUnit(campUnit);
        ensureZoneBelongsToSite(campUnit.getZoneId(), site.getId());
        campUnit.setCampSiteId(site.getId());
        campUnitMapper.updateUnit(campUnit);
    }

    @Override
    public void deleteUnit(Integer id) {
        CampSite site = getActiveSite();
        if (site == null) {
            throw new IllegalArgumentException("找不到營區資料");
        }

        CampUnit existingUnit = campUnitMapper.findById(id);
        if (existingUnit == null || !site.getId().equals(existingUnit.getCampSiteId())) {
            throw new IllegalArgumentException("找不到營位資料");
        }

        Integer ratePlanCount = campUnitMapper.countRatePlansByUnitId(id);
        Integer availabilityCount = campUnitMapper.countAvailabilityByUnitId(id);
        Integer bookingItemCount = campUnitMapper.countBookingItemsByUnitId(id);
        if ((ratePlanCount != null && ratePlanCount > 0)
                || (availabilityCount != null && availabilityCount > 0)
                || (bookingItemCount != null && bookingItemCount > 0)) {
            throw new IllegalArgumentException("此營位已有價格、庫存或預約資料，請改用停用保留歷史資料");
        }

        campUnitMapper.deleteUnit(id, site.getId());
    }

    @Override
    public void createRatePlan(RatePlan ratePlan) {
        CampSite site = getActiveSite();
        if (site == null) {
            throw new IllegalArgumentException("找不到營區資料");
        }

        normalizeRatePlan(ratePlan);
        ensureUnitBelongsToSite(ratePlan.getCampUnitId(), site.getId());
        ratePlan.setCampSiteId(site.getId());
        ratePlanMapper.insertRatePlan(ratePlan);
    }

    @Override
    public void updateRatePlan(RatePlan ratePlan) {
        CampSite site = getActiveSite();
        if (site == null) {
            throw new IllegalArgumentException("找不到營區資料");
        }
        if (ratePlan.getId() == null) {
            throw new IllegalArgumentException("缺少價格方案 ID");
        }

        RatePlan existingRatePlan = ratePlanMapper.findById(ratePlan.getId());
        if (existingRatePlan == null || !site.getId().equals(existingRatePlan.getCampSiteId())) {
            throw new IllegalArgumentException("找不到價格方案");
        }

        normalizeRatePlan(ratePlan);
        ensureUnitBelongsToSite(ratePlan.getCampUnitId(), site.getId());
        ratePlan.setCampSiteId(site.getId());
        ratePlanMapper.updateRatePlan(ratePlan);
    }

    @Override
    public void deleteRatePlan(Integer id) {
        CampSite site = getActiveSite();
        if (site == null) {
            throw new IllegalArgumentException("找不到營區資料");
        }

        RatePlan existingRatePlan = ratePlanMapper.findById(id);
        if (existingRatePlan == null || !site.getId().equals(existingRatePlan.getCampSiteId())) {
            throw new IllegalArgumentException("找不到價格方案");
        }

        Integer bookingItemCount = ratePlanMapper.countBookingItemsByRatePlanId(id);
        if (bookingItemCount != null && bookingItemCount > 0) {
            throw new IllegalArgumentException("此價格方案已有預約明細，請改用停用保留歷史資料");
        }
        ratePlanMapper.deleteRatePlan(id, site.getId());
    }

    @Override
    public void createAvailability(Availability availability) {
        CampSite site = getActiveSite();
        if (site == null) {
            throw new IllegalArgumentException("找不到營區資料");
        }

        normalizeAvailability(availability);
        ensureUnitBelongsToSite(availability.getCampUnitId(), site.getId());
        ensureUniqueAvailability(availability.getCampUnitId(), availability.getAvailableDate(), null);
        availability.setCampSiteId(site.getId());
        availabilityMapper.insertAvailability(availability);
    }

    @Override
    public void updateAvailability(Availability availability) {
        CampSite site = getActiveSite();
        if (site == null) {
            throw new IllegalArgumentException("找不到營區資料");
        }
        if (availability.getId() == null) {
            throw new IllegalArgumentException("缺少庫存 ID");
        }

        Availability existingAvailability = availabilityMapper.findById(availability.getId());
        if (existingAvailability == null || !site.getId().equals(existingAvailability.getCampSiteId())) {
            throw new IllegalArgumentException("找不到庫存資料");
        }

        normalizeAvailability(availability);
        ensureUnitBelongsToSite(availability.getCampUnitId(), site.getId());
        ensureUniqueAvailability(availability.getCampUnitId(), availability.getAvailableDate(), availability.getId());
        availability.setCampSiteId(site.getId());
        availabilityMapper.updateAvailability(availability);
    }

    @Override
    public void deleteAvailability(Integer id) {
        CampSite site = getActiveSite();
        if (site == null) {
            throw new IllegalArgumentException("找不到營區資料");
        }

        Availability existingAvailability = availabilityMapper.findById(id);
        if (existingAvailability == null || !site.getId().equals(existingAvailability.getCampSiteId())) {
            throw new IllegalArgumentException("找不到庫存資料");
        }
        if (existingAvailability.getReservedQuantity() != null && existingAvailability.getReservedQuantity() > 0) {
            throw new IllegalArgumentException("此日期已有保留數量，請改用關閉或調整庫存");
        }
        availabilityMapper.deleteAvailability(id, site.getId());
    }

    @Override
    public void createRentalItem(RentalItem rentalItem) {
        CampSite site = getActiveSite();
        if (site == null) {
            throw new IllegalArgumentException("找不到營區資料");
        }

        normalizeRentalItem(rentalItem);
        rentalItem.setCampSiteId(site.getId());
        rentalItemMapper.insertRentalItem(rentalItem);
    }

    @Override
    public void updateRentalItem(RentalItem rentalItem) {
        CampSite site = getActiveSite();
        if (site == null) {
            throw new IllegalArgumentException("找不到營區資料");
        }
        if (rentalItem.getId() == null) {
            throw new IllegalArgumentException("缺少租借品 ID");
        }

        RentalItem existingRentalItem = rentalItemMapper.findById(rentalItem.getId());
        if (existingRentalItem == null || !site.getId().equals(existingRentalItem.getCampSiteId())) {
            throw new IllegalArgumentException("找不到租借品資料");
        }

        normalizeRentalItem(rentalItem);
        rentalItem.setCampSiteId(site.getId());
        rentalItemMapper.updateRentalItem(rentalItem);
    }

    @Override
    public void deleteRentalItem(Integer id) {
        CampSite site = getActiveSite();
        if (site == null) {
            throw new IllegalArgumentException("找不到營區資料");
        }

        RentalItem existingRentalItem = rentalItemMapper.findById(id);
        if (existingRentalItem == null || !site.getId().equals(existingRentalItem.getCampSiteId())) {
            throw new IllegalArgumentException("找不到租借品資料");
        }
        rentalItemMapper.deleteRentalItem(id, site.getId());
    }

    @Override
    public void createAnnouncement(Announcement announcement, Integer currentUserId) {
        CampSite site = getActiveSite();
        if (site == null) {
            throw new IllegalArgumentException("找不到營區資料");
        }

        normalizeAnnouncement(announcement);
        announcement.setCampSiteId(site.getId());
        announcement.setCreatedBy(currentUserId);
        announcementMapper.insertAnnouncement(announcement);
    }

    @Override
    public void updateAnnouncement(Announcement announcement) {
        CampSite site = getActiveSite();
        if (site == null) {
            throw new IllegalArgumentException("找不到營區資料");
        }
        if (announcement.getId() == null) {
            throw new IllegalArgumentException("缺少公告 ID");
        }

        Announcement existingAnnouncement = announcementMapper.findById(announcement.getId());
        if (existingAnnouncement == null || !site.getId().equals(existingAnnouncement.getCampSiteId())) {
            throw new IllegalArgumentException("找不到公告資料");
        }

        normalizeAnnouncement(announcement);
        announcement.setCampSiteId(site.getId());
        announcement.setCreatedBy(existingAnnouncement.getCreatedBy());
        announcementMapper.updateAnnouncement(announcement);
    }

    @Override
    public void deleteAnnouncement(Integer id) {
        CampSite site = getActiveSite();
        if (site == null) {
            throw new IllegalArgumentException("找不到營區資料");
        }

        Announcement existingAnnouncement = announcementMapper.findById(id);
        if (existingAnnouncement == null || !site.getId().equals(existingAnnouncement.getCampSiteId())) {
            throw new IllegalArgumentException("找不到公告資料");
        }
        announcementMapper.deleteAnnouncement(id, site.getId());
    }

    @Override
    public void createFaq(Faq faq) {
        CampSite site = getActiveSite();
        if (site == null) {
            throw new IllegalArgumentException("找不到營區資料");
        }

        normalizeFaq(faq);
        faq.setCampSiteId(site.getId());
        faqMapper.insertFaq(faq);
    }

    @Override
    public void updateFaq(Faq faq) {
        CampSite site = getActiveSite();
        if (site == null) {
            throw new IllegalArgumentException("找不到營區資料");
        }
        if (faq.getId() == null) {
            throw new IllegalArgumentException("缺少 FAQ ID");
        }

        Faq existingFaq = faqMapper.findById(faq.getId());
        if (existingFaq == null || !site.getId().equals(existingFaq.getCampSiteId())) {
            throw new IllegalArgumentException("找不到 FAQ 資料");
        }

        normalizeFaq(faq);
        faq.setCampSiteId(site.getId());
        faqMapper.updateFaq(faq);
    }

    @Override
    public void deleteFaq(Integer id) {
        CampSite site = getActiveSite();
        if (site == null) {
            throw new IllegalArgumentException("找不到營區資料");
        }

        Faq existingFaq = faqMapper.findById(id);
        if (existingFaq == null || !site.getId().equals(existingFaq.getCampSiteId())) {
            throw new IllegalArgumentException("找不到 FAQ 資料");
        }
        faqMapper.deleteFaq(id, site.getId());
    }

    @Override
    public void createSiteImage(SiteImage siteImage) {
        CampSite site = getActiveSite();
        if (site == null) {
            throw new IllegalArgumentException("找不到營區資料");
        }

        normalizeSiteImage(siteImage);
        if (siteImage.getZoneId() != null) {
            ensureZoneBelongsToSite(siteImage.getZoneId(), site.getId());
        }
        siteImage.setCampSiteId(site.getId());
        siteImageMapper.insertImage(siteImage);
        if (Boolean.TRUE.equals(siteImage.getIsCover()) && siteImage.getId() != null) {
            siteImageMapper.clearOtherCoverImages(site.getId(), siteImage.getId());
        }
    }

    @Override
    public void updateSiteImage(SiteImage siteImage) {
        CampSite site = getActiveSite();
        if (site == null) {
            throw new IllegalArgumentException("找不到營區資料");
        }
        if (siteImage.getId() == null) {
            throw new IllegalArgumentException("缺少圖片 ID");
        }

        SiteImage existingSiteImage = siteImageMapper.findById(siteImage.getId());
        if (existingSiteImage == null || !site.getId().equals(existingSiteImage.getCampSiteId())) {
            throw new IllegalArgumentException("找不到相簿圖片");
        }

        normalizeSiteImage(siteImage);
        if (siteImage.getZoneId() != null) {
            ensureZoneBelongsToSite(siteImage.getZoneId(), site.getId());
        }
        siteImage.setCampSiteId(site.getId());
        siteImageMapper.updateImage(siteImage);
        if (Boolean.TRUE.equals(siteImage.getIsCover())) {
            siteImageMapper.clearOtherCoverImages(site.getId(), siteImage.getId());
        }
    }

    @Override
    public void deleteSiteImage(Integer id) {
        CampSite site = getActiveSite();
        if (site == null) {
            throw new IllegalArgumentException("找不到營區資料");
        }

        SiteImage existingSiteImage = siteImageMapper.findById(id);
        if (existingSiteImage == null || !site.getId().equals(existingSiteImage.getCampSiteId())) {
            throw new IllegalArgumentException("找不到相簿圖片");
        }
        siteImageMapper.deleteImage(id, site.getId());
    }

    private void normalizeUnit(CampUnit campUnit) {
        if (campUnit.getZoneId() == null) {
            throw new IllegalArgumentException("請選擇所屬區域");
        }
        if (campUnit.getName() == null || campUnit.getName().isBlank()) {
            throw new IllegalArgumentException("請填寫營位名稱");
        }
        if (campUnit.getUnitType() == null || campUnit.getUnitType().isBlank()) {
            campUnit.setUnitType("tent_site");
        }
        if (campUnit.getMaxGuests() == null || campUnit.getMaxGuests() < 1) {
            campUnit.setMaxGuests(1);
        }
        if (campUnit.getMaxVehicles() == null || campUnit.getMaxVehicles() < 0) {
            campUnit.setMaxVehicles(0);
        }
        if (campUnit.getMaxTents() == null || campUnit.getMaxTents() < 0) {
            campUnit.setMaxTents(0);
        }
        if (campUnit.getQuantity() == null || campUnit.getQuantity() < 1) {
            campUnit.setQuantity(1);
        }
        if (campUnit.getSortOrder() == null) {
            campUnit.setSortOrder(0);
        }
        if (campUnit.getHasShelter() == null) {
            campUnit.setHasShelter(false);
        }
        if (campUnit.getElectricityAvailable() == null) {
            campUnit.setElectricityAvailable(false);
        }
        if (campUnit.getPrivateBathroom() == null) {
            campUnit.setPrivateBathroom(false);
        }
        if (campUnit.getIsActive() == null) {
            campUnit.setIsActive(true);
        }
    }

    private void ensureZoneBelongsToSite(Integer zoneId, Integer campSiteId) {
        CampZone zone = campZoneMapper.findById(zoneId);
        if (zone == null || !campSiteId.equals(zone.getCampSiteId())) {
            throw new IllegalArgumentException("所屬區域不存在");
        }
    }

    private void normalizeRatePlan(RatePlan ratePlan) {
        if (ratePlan.getCampUnitId() == null) {
            throw new IllegalArgumentException("請選擇營位");
        }
        if (ratePlan.getName() == null || ratePlan.getName().isBlank()) {
            throw new IllegalArgumentException("請填寫價格方案名稱");
        }
        if (ratePlan.getDayType() == null || ratePlan.getDayType().isBlank()) {
            ratePlan.setDayType("weekday");
        }
        if (ratePlan.getBasePrice() == null || ratePlan.getBasePrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("基本價格不可小於 0");
        }
        if (ratePlan.getExtraPersonPrice() == null) {
            ratePlan.setExtraPersonPrice(BigDecimal.ZERO);
        }
        if (ratePlan.getCleaningFee() == null) {
            ratePlan.setCleaningFee(BigDecimal.ZERO);
        }
        if (ratePlan.getExtraPersonPrice().compareTo(BigDecimal.ZERO) < 0
                || ratePlan.getCleaningFee().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("加人費與清潔費不可小於 0");
        }
        if (ratePlan.getIsActive() == null) {
            ratePlan.setIsActive(true);
        }
    }

    private void normalizeAvailability(Availability availability) {
        if (availability.getCampUnitId() == null) {
            throw new IllegalArgumentException("請選擇營位");
        }
        if (availability.getAvailableDate() == null) {
            throw new IllegalArgumentException("請選擇日期");
        }
        if (availability.getTotalQuantity() == null || availability.getTotalQuantity() < 0) {
            availability.setTotalQuantity(0);
        }
        if (availability.getReservedQuantity() == null || availability.getReservedQuantity() < 0) {
            availability.setReservedQuantity(0);
        }
        if (availability.getBlockedQuantity() == null || availability.getBlockedQuantity() < 0) {
            availability.setBlockedQuantity(0);
        }
        if (availability.getReservedQuantity() + availability.getBlockedQuantity() > availability.getTotalQuantity()) {
            throw new IllegalArgumentException("已保留與封鎖數量不可超過總量");
        }
        if (availability.getPriceOverride() != null && availability.getPriceOverride().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("覆寫價格不可小於 0");
        }
        if (availability.getIsClosed() == null) {
            availability.setIsClosed(false);
        }
    }

    private void normalizeRentalItem(RentalItem rentalItem) {
        if (rentalItem.getName() == null || rentalItem.getName().isBlank()) {
            throw new IllegalArgumentException("請填寫租借品名稱");
        }
        if (rentalItem.getCategory() == null || rentalItem.getCategory().isBlank()) {
            rentalItem.setCategory("other");
        }
        if (rentalItem.getUnitPrice() == null || rentalItem.getUnitPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("租借單價不可小於 0");
        }
        if (rentalItem.getTotalQuantity() == null || rentalItem.getTotalQuantity() < 0) {
            rentalItem.setTotalQuantity(0);
        }
        if (rentalItem.getSortOrder() == null) {
            rentalItem.setSortOrder(0);
        }
        if (rentalItem.getIsActive() == null) {
            rentalItem.setIsActive(true);
        }
    }

    private void normalizeAnnouncement(Announcement announcement) {
        if (announcement.getTitle() == null || announcement.getTitle().isBlank()) {
            throw new IllegalArgumentException("請填寫公告標題");
        }
        if (announcement.getContent() == null || announcement.getContent().isBlank()) {
            throw new IllegalArgumentException("請填寫公告內容");
        }
        if (announcement.getPublishStartTime() != null
                && announcement.getPublishEndTime() != null
                && announcement.getPublishEndTime().isBefore(announcement.getPublishStartTime())) {
            throw new IllegalArgumentException("公告結束時間不可早於開始時間");
        }
        if (announcement.getIsPinned() == null) {
            announcement.setIsPinned(false);
        }
        if (announcement.getIsActive() == null) {
            announcement.setIsActive(true);
        }
    }

    private void normalizeFaq(Faq faq) {
        if (faq.getQuestion() == null || faq.getQuestion().isBlank()) {
            throw new IllegalArgumentException("請填寫 FAQ 問題");
        }
        if (faq.getAnswer() == null || faq.getAnswer().isBlank()) {
            throw new IllegalArgumentException("請填寫 FAQ 回答");
        }
        if (faq.getSortOrder() == null) {
            faq.setSortOrder(0);
        }
        if (faq.getIsActive() == null) {
            faq.setIsActive(true);
        }
    }

    private void normalizeSiteImage(SiteImage siteImage) {
        if (siteImage.getImageUrl() == null || siteImage.getImageUrl().isBlank()) {
            throw new IllegalArgumentException("請填寫圖片網址");
        }
        if (siteImage.getTitle() == null || siteImage.getTitle().isBlank()) {
            throw new IllegalArgumentException("請填寫圖片標題");
        }
        if (siteImage.getAltText() == null || siteImage.getAltText().isBlank()) {
            siteImage.setAltText(siteImage.getTitle());
        }
        if (siteImage.getSortOrder() == null) {
            siteImage.setSortOrder(0);
        }
        if (siteImage.getIsCover() == null) {
            siteImage.setIsCover(false);
        }
        if (siteImage.getIsActive() == null) {
            siteImage.setIsActive(true);
        }
    }

    private void ensureUnitBelongsToSite(Integer unitId, Integer campSiteId) {
        CampUnit unit = campUnitMapper.findById(unitId);
        if (unit == null || !campSiteId.equals(unit.getCampSiteId())) {
            throw new IllegalArgumentException("營位不存在");
        }
    }

    private void ensureUniqueAvailability(Integer campUnitId, LocalDate availableDate, Integer excludeId) {
        Integer count = availabilityMapper.countByUnitAndDate(campUnitId, availableDate, excludeId);
        if (count != null && count > 0) {
            throw new IllegalArgumentException("此營位日期已有庫存資料");
        }
    }
}
