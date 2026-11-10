package com.fadaiting.camp.mapper;

import com.fadaiting.camp.pojo.CampUnit;
import com.fadaiting.camp.pojo.CampZone;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface CampZoneMapper {
    @Select("""
            select *
            from camp_zone
            where camp_site_id = #{campSiteId}
              and is_active = true
            order by sort_order, id
            """)
    List<CampZone> findZonesBySiteId(Integer campSiteId);

    @Select("""
            select *
            from camp_zone
            where camp_site_id = #{campSiteId}
            order by sort_order, id
            """)
    List<CampZone> findAdminZonesBySiteId(Integer campSiteId);

    @Select("""
            select *
            from camp_zone
            where id = #{id}
            """)
    CampZone findById(Integer id);

    @Select("""
            select count(1)
            from camp_zone
            where camp_site_id = #{campSiteId}
              and slug = #{slug}
              and (cast(#{excludeId,jdbcType=INTEGER} as integer) is null or id <> #{excludeId,jdbcType=INTEGER})
            """)
    Integer countBySlug(
            @Param("campSiteId") Integer campSiteId,
            @Param("slug") String slug,
            @Param("excludeId") Integer excludeId
    );

    @Select("""
            select *
            from camp_unit
            where zone_id = #{zoneId}
              and is_active = true
            order by sort_order, id
            """)
    List<CampUnit> findUnitsByZoneId(Integer zoneId);

    @Select("""
            select count(1)
            from camp_unit
            where zone_id = #{zoneId}
            """)
    Integer countUnitsByZoneId(Integer zoneId);

    @Insert("""
            insert into camp_zone (
                camp_site_id,
                name,
                slug,
                zone_type,
                description,
                sort_order,
                is_active
            )
            values (
                #{campSiteId},
                #{name},
                #{slug},
                #{zoneType},
                #{description},
                #{sortOrder},
                #{isActive}
            )
            """)
    void insertZone(CampZone campZone);

    @Update("""
            update camp_zone
            set name = #{name},
                slug = #{slug},
                zone_type = #{zoneType},
                description = #{description},
                sort_order = #{sortOrder},
                is_active = #{isActive},
                modified_time = now()
            where id = #{id}
              and camp_site_id = #{campSiteId}
            """)
    void updateZone(CampZone campZone);

    @Delete("""
            delete from camp_zone
            where id = #{id}
              and camp_site_id = #{campSiteId}
            """)
    void deleteZone(@Param("id") Integer id, @Param("campSiteId") Integer campSiteId);
}
