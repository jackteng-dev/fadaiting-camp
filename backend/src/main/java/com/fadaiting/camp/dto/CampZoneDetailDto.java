package com.fadaiting.camp.dto;

import com.fadaiting.camp.pojo.CampUnit;
import com.fadaiting.camp.pojo.CampZone;
import lombok.Data;

import java.util.List;

@Data
public class CampZoneDetailDto {
    private CampZone zone;
    private List<CampUnit> units;
}
