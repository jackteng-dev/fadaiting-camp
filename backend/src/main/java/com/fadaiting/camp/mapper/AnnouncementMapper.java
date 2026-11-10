package com.fadaiting.camp.mapper;

import com.fadaiting.camp.pojo.Announcement;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface AnnouncementMapper {
    @Select("""
            select *
            from announcement
            where camp_site_id = #{campSiteId}
            order by is_pinned desc,
                     coalesce(publish_start_time, created_time) desc,
                     id desc
            """)
    List<Announcement> findAdminAnnouncementsBySiteId(Integer campSiteId);

    @Select("""
            select *
            from announcement
            where camp_site_id = #{campSiteId}
              and is_active = true
              and (publish_start_time is null or publish_start_time <= now())
              and (publish_end_time is null or publish_end_time >= now())
            order by is_pinned desc, created_time desc, id desc
            """)
    List<Announcement> findActiveAnnouncementsBySiteId(Integer campSiteId);

    @Select("""
            select *
            from announcement
            where id = #{id}
            """)
    Announcement findById(Integer id);

    @Insert("""
            insert into announcement (
                camp_site_id,
                title,
                content,
                publish_start_time,
                publish_end_time,
                is_pinned,
                is_active,
                created_by
            )
            values (
                #{campSiteId},
                #{title},
                #{content},
                #{publishStartTime},
                #{publishEndTime},
                #{isPinned},
                #{isActive},
                #{createdBy}
            )
            """)
    void insertAnnouncement(Announcement announcement);

    @Update("""
            update announcement
            set title = #{title},
                content = #{content},
                publish_start_time = #{publishStartTime},
                publish_end_time = #{publishEndTime},
                is_pinned = #{isPinned},
                is_active = #{isActive},
                modified_time = now()
            where id = #{id}
              and camp_site_id = #{campSiteId}
            """)
    void updateAnnouncement(Announcement announcement);

    @Delete("""
            delete from announcement
            where id = #{id}
              and camp_site_id = #{campSiteId}
            """)
    void deleteAnnouncement(@Param("id") Integer id, @Param("campSiteId") Integer campSiteId);
}
