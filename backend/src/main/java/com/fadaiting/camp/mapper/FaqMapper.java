package com.fadaiting.camp.mapper;

import com.fadaiting.camp.pojo.Faq;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface FaqMapper {
    @Select("""
            select *
            from faq
            where camp_site_id = #{campSiteId}
            order by sort_order, id
            """)
    List<Faq> findAdminFaqsBySiteId(Integer campSiteId);

    @Select("""
            select *
            from faq
            where id = #{id}
            """)
    Faq findById(Integer id);

    @Insert("""
            insert into faq (
                camp_site_id,
                question,
                answer,
                sort_order,
                is_active
            )
            values (
                #{campSiteId},
                #{question},
                #{answer},
                #{sortOrder},
                #{isActive}
            )
            """)
    void insertFaq(Faq faq);

    @Update("""
            update faq
            set question = #{question},
                answer = #{answer},
                sort_order = #{sortOrder},
                is_active = #{isActive},
                modified_time = now()
            where id = #{id}
              and camp_site_id = #{campSiteId}
            """)
    void updateFaq(Faq faq);

    @Delete("""
            delete from faq
            where id = #{id}
              and camp_site_id = #{campSiteId}
            """)
    void deleteFaq(@Param("id") Integer id, @Param("campSiteId") Integer campSiteId);
}
