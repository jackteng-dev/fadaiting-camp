package com.fadaiting.camp.mapper;

import com.fadaiting.camp.pojo.RatePlan;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface RatePlanMapper {
    @Select("""
            select *
            from rate_plan
            where camp_site_id = #{campSiteId}
            order by camp_unit_id, day_type, id
            """)
    List<RatePlan> findAdminRatePlansBySiteId(Integer campSiteId);

    @Select("""
            select *
            from rate_plan
            where id = #{id}
            """)
    RatePlan findById(Integer id);

    @Select("""
            select count(1)
            from booking_item
            where rate_plan_id = #{ratePlanId}
            """)
    Integer countBookingItemsByRatePlanId(Integer ratePlanId);

    @Insert("""
            insert into rate_plan (
                camp_site_id,
                camp_unit_id,
                name,
                day_type,
                base_price,
                extra_person_price,
                cleaning_fee,
                description,
                is_active
            )
            values (
                #{campSiteId},
                #{campUnitId},
                #{name},
                #{dayType},
                #{basePrice},
                #{extraPersonPrice},
                #{cleaningFee},
                #{description},
                #{isActive}
            )
            """)
    void insertRatePlan(RatePlan ratePlan);

    @Update("""
            update rate_plan
            set camp_unit_id = #{campUnitId},
                name = #{name},
                day_type = #{dayType},
                base_price = #{basePrice},
                extra_person_price = #{extraPersonPrice},
                cleaning_fee = #{cleaningFee},
                description = #{description},
                is_active = #{isActive},
                modified_time = now()
            where id = #{id}
              and camp_site_id = #{campSiteId}
            """)
    void updateRatePlan(RatePlan ratePlan);

    @Delete("""
            delete from rate_plan
            where id = #{id}
              and camp_site_id = #{campSiteId}
            """)
    void deleteRatePlan(@Param("id") Integer id, @Param("campSiteId") Integer campSiteId);
}
