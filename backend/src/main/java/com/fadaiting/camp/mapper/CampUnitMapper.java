package com.fadaiting.camp.mapper;

import com.fadaiting.camp.pojo.CampUnit;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface CampUnitMapper {
    @Select("""
            select *
            from camp_unit
            where camp_site_id = #{campSiteId}
            order by sort_order, id
            """)
    List<CampUnit> findAdminUnitsBySiteId(Integer campSiteId);

    @Select("""
            select *
            from camp_unit
            where id = #{id}
            """)
    CampUnit findById(Integer id);

    @Select("""
            select count(1)
            from rate_plan
            where camp_unit_id = #{campUnitId}
            """)
    Integer countRatePlansByUnitId(Integer campUnitId);

    @Select("""
            select count(1)
            from availability
            where camp_unit_id = #{campUnitId}
            """)
    Integer countAvailabilityByUnitId(Integer campUnitId);

    @Select("""
            select count(1)
            from booking_item
            where camp_unit_id = #{campUnitId}
            """)
    Integer countBookingItemsByUnitId(Integer campUnitId);

    @Insert("""
            insert into camp_unit (
                camp_site_id,
                zone_id,
                name,
                unit_type,
                description,
                max_guests,
                max_vehicles,
                max_tents,
                has_shelter,
                electricity_available,
                private_bathroom,
                quantity,
                sort_order,
                is_active
            )
            values (
                #{campSiteId},
                #{zoneId},
                #{name},
                #{unitType},
                #{description},
                #{maxGuests},
                #{maxVehicles},
                #{maxTents},
                #{hasShelter},
                #{electricityAvailable},
                #{privateBathroom},
                #{quantity},
                #{sortOrder},
                #{isActive}
            )
            """)
    void insertUnit(CampUnit campUnit);

    @Update("""
            update camp_unit
            set zone_id = #{zoneId},
                name = #{name},
                unit_type = #{unitType},
                description = #{description},
                max_guests = #{maxGuests},
                max_vehicles = #{maxVehicles},
                max_tents = #{maxTents},
                has_shelter = #{hasShelter},
                electricity_available = #{electricityAvailable},
                private_bathroom = #{privateBathroom},
                quantity = #{quantity},
                sort_order = #{sortOrder},
                is_active = #{isActive},
                modified_time = now()
            where id = #{id}
              and camp_site_id = #{campSiteId}
            """)
    void updateUnit(CampUnit campUnit);

    @Delete("""
            delete from camp_unit
            where id = #{id}
              and camp_site_id = #{campSiteId}
            """)
    void deleteUnit(@Param("id") Integer id, @Param("campSiteId") Integer campSiteId);
}
