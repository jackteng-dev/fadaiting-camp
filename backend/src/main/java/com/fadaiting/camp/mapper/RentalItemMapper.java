package com.fadaiting.camp.mapper;

import com.fadaiting.camp.pojo.RentalItem;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.time.LocalDate;

@Mapper
public interface RentalItemMapper {
    @Select("select * from rental_item where id = #{id} for update")
    RentalItem findForUpdate(Integer id);

    @Select("""
            select *
            from rental_item
            where camp_site_id = #{campSiteId}
            order by sort_order, id
            """)
    List<RentalItem> findAdminRentalItemsBySiteId(Integer campSiteId);

    @Select("""
            select *
            from rental_item
            where camp_site_id = #{campSiteId}
              and is_active = true
            order by sort_order, id
            """)
    List<RentalItem> findActiveRentalItemsBySiteId(Integer campSiteId);

    @Select("""
            select *
            from rental_item
            where id = #{id}
            """)
    RentalItem findById(Integer id);

    @Select("""
            <script>
            select *
            from rental_item
            where camp_site_id = #{campSiteId}
              and is_active = true
              and id in
              <foreach collection="ids" item="id" open="(" separator="," close=")">
                #{id}
              </foreach>
            </script>
            """)
    List<RentalItem> findActiveRentalItemsByIds(@Param("campSiteId") Integer campSiteId, @Param("ids") List<Integer> ids);

    @Select("""
            select coalesce(max(day_reserved.reserved_quantity), 0)::int
            from (
                select coalesce(sum(bi.quantity), 0) as reserved_quantity
                from generate_series(
                    cast(#{checkInDate,jdbcType=DATE} as date),
                    cast(#{checkOutDate,jdbcType=DATE} as date) - interval '1 day',
                    interval '1 day'
                ) as stay_day(day_date)
                left join booking b
                  on b.check_in_date <= stay_day.day_date
                 and b.check_out_date > stay_day.day_date
                 and b.status <> 'cancelled'
                left join booking_item bi
                  on bi.booking_id = b.id
                 and bi.rental_item_id = #{rentalItemId}
                group by stay_day.day_date
            ) day_reserved
            """)
    Integer findMaxReservedQuantityByRentalItemAndDateRange(
            @Param("rentalItemId") Integer rentalItemId,
            @Param("checkInDate") LocalDate checkInDate,
            @Param("checkOutDate") LocalDate checkOutDate
    );

    @Insert("""
            insert into rental_item (
                camp_site_id,
                name,
                category,
                description,
                image_url,
                unit_price,
                total_quantity,
                sort_order,
                is_active
            )
            values (
                #{campSiteId},
                #{name},
                #{category},
                #{description},
                #{imageUrl},
                #{unitPrice},
                #{totalQuantity},
                #{sortOrder},
                #{isActive}
            )
            """)
    void insertRentalItem(RentalItem rentalItem);

    @Update("""
            update rental_item
            set name = #{name},
                category = #{category},
                description = #{description},
                image_url = #{imageUrl},
                unit_price = #{unitPrice},
                total_quantity = #{totalQuantity},
                sort_order = #{sortOrder},
                is_active = #{isActive},
                modified_time = now()
            where id = #{id}
              and camp_site_id = #{campSiteId}
            """)
    void updateRentalItem(RentalItem rentalItem);

    @Delete("""
            delete from rental_item
            where id = #{id}
              and camp_site_id = #{campSiteId}
            """)
    void deleteRentalItem(@Param("id") Integer id, @Param("campSiteId") Integer campSiteId);
}
