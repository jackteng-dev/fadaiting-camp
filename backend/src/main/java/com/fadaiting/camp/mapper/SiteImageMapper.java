package com.fadaiting.camp.mapper;

import com.fadaiting.camp.pojo.SiteImage;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface SiteImageMapper {
    @Select("""
            select si.*,
                   cz.name as zone_name
            from site_image si
            left join camp_zone cz on cz.id = si.zone_id
            where si.camp_site_id = #{campSiteId}
            order by si.is_cover desc,
                     si.sort_order,
                     si.id
            """)
    List<SiteImage> findAdminImagesBySiteId(Integer campSiteId);

    @Select("""
            select *
            from site_image
            where id = #{id}
            """)
    SiteImage findById(Integer id);

    @Insert("""
            insert into site_image (
                camp_site_id,
                zone_id,
                title,
                image_url,
                alt_text,
                sort_order,
                is_cover,
                is_active
            )
            values (
                #{campSiteId},
                #{zoneId},
                #{title},
                #{imageUrl},
                #{altText},
                #{sortOrder},
                #{isCover},
                #{isActive}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertImage(SiteImage siteImage);

    @Update("""
            update site_image
            set zone_id = #{zoneId},
                title = #{title},
                image_url = #{imageUrl},
                alt_text = #{altText},
                sort_order = #{sortOrder},
                is_cover = #{isCover},
                is_active = #{isActive},
                modified_time = now()
            where id = #{id}
              and camp_site_id = #{campSiteId}
            """)
    void updateImage(SiteImage siteImage);

    @Update("""
            update site_image
            set is_cover = false,
                modified_time = now()
            where camp_site_id = #{campSiteId}
              and id <> #{excludeId}
            """)
    void clearOtherCoverImages(@Param("campSiteId") Integer campSiteId, @Param("excludeId") Integer excludeId);

    @Delete("""
            delete from site_image
            where id = #{id}
              and camp_site_id = #{campSiteId}
            """)
    void deleteImage(@Param("id") Integer id, @Param("campSiteId") Integer campSiteId);
}
