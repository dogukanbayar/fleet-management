package com.filo.fleet.mapper;

import com.filo.fleet.dto.request.DriverRequest;
import com.filo.fleet.dto.response.DriverResponse;
import com.filo.fleet.entity.Driver;
import java.util.List;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface DriverMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "assignments", ignore = true)
    // "active" alanı istekte opsiyoneldir (Boolean, null olabilir); entity'deki primitif boolean'a
    // otomatik unboxing sırasında NPE oluşmaması için burada atlanır ve serviste açıkça atanır.
    @Mapping(target = "active", ignore = true)
    Driver toEntity(DriverRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "assignments", ignore = true)
    @Mapping(target = "active", ignore = true)
    void updateEntity(DriverRequest request, @MappingTarget Driver driver);

    DriverResponse toResponse(Driver driver);

    List<DriverResponse> toResponseList(List<Driver> drivers);
}
