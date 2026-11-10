package com.fadaiting.camp.mapper;

import com.fadaiting.camp.pojo.CampSite;
import com.fadaiting.camp.pojo.Facility;
import com.fadaiting.camp.pojo.Faq;
import com.fadaiting.camp.pojo.SiteImage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface CampSiteMapper {
    @Select("""
            select *
            from camp_site
            where is_active = true
            order by id
            limit 1
            """)
    CampSite findActiveSite();

    @Select("""
            select *
            from facility
            where camp_site_id = #{campSiteId}
              and is_active = true
            order by sort_order, id
            """)
    List<Facility> findFacilitiesBySiteId(Integer campSiteId);

    @Select("""
            select *
            from faq
            where camp_site_id = #{campSiteId}
              and is_active = true
            order by sort_order, id
            """)
    List<Faq> findFaqsBySiteId(Integer campSiteId);

    @Select("""
            select *
            from site_image
            where camp_site_id = #{campSiteId}
              and is_active = true
            order by is_cover desc, sort_order, id
            """)
    List<SiteImage> findImagesBySiteId(Integer campSiteId);

    @Update("""
            update camp_site
            set name = #{name},
                slug = #{slug},
                short_description = #{shortDescription},
                full_description = #{fullDescription},
                address_line = #{addressLine},
                city = #{city},
                county = #{county},
                postal_code = #{postalCode},
                latitude = #{latitude},
                longitude = #{longitude},
                contact_name = #{contactName},
                contact_phone = #{contactPhone},
                contact_email = #{contactEmail},
                facebook_url = #{facebookUrl},
                check_in_time = #{checkInTime},
                check_out_time = #{checkOutTime},
                booking_notice = #{bookingNotice},
                refund_policy = #{refundPolicy},
                is_active = #{isActive},
                modified_time = now()
            where id = #{id}
            """)
    void updateSite(CampSite campSite);
}
