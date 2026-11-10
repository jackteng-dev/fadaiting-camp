package com.fadaiting.camp.mapper;

import com.fadaiting.camp.pojo.Availability;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface AvailabilityMapper {
    @Insert("""
            insert into availability(camp_site_id, camp_unit_id, available_date, total_quantity, reserved_quantity, blocked_quantity, is_closed)
            select #{campSiteId}, #{campUnitId}, #{date}, #{total}, #{quantity}, 0, false where #{total} >= #{quantity}
            on conflict(camp_unit_id, available_date) do update
            set reserved_quantity = availability.reserved_quantity + #{quantity}, modified_time = now()
            where availability.is_closed = false
              and availability.reserved_quantity + availability.blocked_quantity + #{quantity} <= availability.total_quantity
            """)
    int reserveIfAvailable(@Param("campSiteId") Integer campSiteId, @Param("campUnitId") Integer campUnitId,
            @Param("date") LocalDate date, @Param("total") Integer total, @Param("quantity") int quantity);

    @Update("""
            update availability set reserved_quantity = reserved_quantity - #{quantity}, modified_time = now()
            where camp_unit_id = #{campUnitId} and available_date = #{date} and reserved_quantity >= #{quantity}
            """)
    int releaseReserved(@Param("campUnitId") Integer campUnitId, @Param("date") LocalDate date, @Param("quantity") int quantity);

    @Select("""
            select *
            from availability
            where camp_site_id = #{campSiteId}
              and (cast(#{campUnitId,jdbcType=INTEGER} as integer) is null or camp_unit_id = #{campUnitId,jdbcType=INTEGER})
              and (cast(#{startDate,jdbcType=DATE} as date) is null or available_date >= #{startDate,jdbcType=DATE})
              and (cast(#{endDate,jdbcType=DATE} as date) is null or available_date <= #{endDate,jdbcType=DATE})
            order by available_date, camp_unit_id, id
            """)
    List<Availability> findAdminAvailability(
            @Param("campSiteId") Integer campSiteId,
            @Param("campUnitId") Integer campUnitId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Select("""
            select *
            from availability
            where id = #{id}
            """)
    Availability findById(Integer id);

    @Select("""
            select *
            from availability
            where camp_unit_id = #{campUnitId}
              and available_date >= #{startDate}
              and available_date < #{endDate}
            order by available_date
            """)
    List<Availability> findByUnitAndDateRange(
            @Param("campUnitId") Integer campUnitId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Select("""
            select count(1)
            from availability
            where camp_unit_id = #{campUnitId}
              and available_date = #{availableDate}
              and (cast(#{excludeId,jdbcType=INTEGER} as integer) is null or id <> #{excludeId,jdbcType=INTEGER})
            """)
    Integer countByUnitAndDate(
            @Param("campUnitId") Integer campUnitId,
            @Param("availableDate") LocalDate availableDate,
            @Param("excludeId") Integer excludeId
    );

    @Insert("""
            insert into availability (
                camp_site_id,
                camp_unit_id,
                available_date,
                total_quantity,
                reserved_quantity,
                blocked_quantity,
                price_override,
                is_closed
            )
            values (
                #{campSiteId},
                #{campUnitId},
                #{availableDate},
                #{totalQuantity},
                #{reservedQuantity},
                #{blockedQuantity},
                #{priceOverride},
                #{isClosed}
            )
            """)
    void insertAvailability(Availability availability);

    @Update("""
            update availability
            set camp_unit_id = #{campUnitId},
                available_date = #{availableDate},
                total_quantity = #{totalQuantity},
                reserved_quantity = #{reservedQuantity},
                blocked_quantity = #{blockedQuantity},
                price_override = #{priceOverride},
                is_closed = #{isClosed},
                modified_time = now()
            where id = #{id}
              and camp_site_id = #{campSiteId}
            """)
    void updateAvailability(Availability availability);

    @Update("""
            update availability
            set reserved_quantity = reserved_quantity + #{delta},
                modified_time = now()
            where camp_unit_id = #{campUnitId}
              and available_date = #{availableDate}
            """)
    void adjustReservedQuantity(
            @Param("campUnitId") Integer campUnitId,
            @Param("availableDate") LocalDate availableDate,
            @Param("delta") Integer delta
    );

    @Insert("""
            insert into availability (
                camp_site_id,
                camp_unit_id,
                available_date,
                total_quantity,
                reserved_quantity,
                blocked_quantity,
                price_override,
                is_closed
            )
            values (
                #{campSiteId},
                #{campUnitId},
                #{availableDate},
                #{totalQuantity},
                greatest(#{delta}, 0),
                0,
                null,
                false
            )
            on conflict (camp_unit_id, available_date)
            do update
            set reserved_quantity = greatest(availability.reserved_quantity + #{delta}, 0),
                modified_time = now()
            """)
    void upsertReservedQuantity(
            @Param("campSiteId") Integer campSiteId,
            @Param("campUnitId") Integer campUnitId,
            @Param("availableDate") LocalDate availableDate,
            @Param("totalQuantity") Integer totalQuantity,
            @Param("delta") Integer delta
    );

    @Delete("""
            delete from availability
            where id = #{id}
              and camp_site_id = #{campSiteId}
            """)
    void deleteAvailability(@Param("id") Integer id, @Param("campSiteId") Integer campSiteId);
}
