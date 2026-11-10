package com.fadaiting.camp.dto;

import com.fadaiting.camp.pojo.CampSite;
import com.fadaiting.camp.pojo.Facility;
import com.fadaiting.camp.pojo.Faq;
import com.fadaiting.camp.pojo.SiteImage;
import lombok.Data;

import java.util.List;

@Data
public class CampSiteDetailDto {
    private CampSite site;
    private List<Facility> facilities;
    private List<Faq> faqs;
    private List<SiteImage> images;
}
