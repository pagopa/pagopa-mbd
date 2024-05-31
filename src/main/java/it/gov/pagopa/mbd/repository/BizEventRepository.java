package it.gov.pagopa.mbd.repository;

import com.azure.spring.data.cosmos.repository.CosmosRepository;
import com.azure.spring.data.cosmos.repository.Query;
import it.gov.pagopa.mbd.repository.model.BizEventEntity;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BizEventRepository extends CosmosRepository<BizEventEntity, String> {

    @Query("SELECT be FROM BizEventEntity be JOIN be.transferList tl WHERE be.timestamp >= :dateFrom and be.timestamp <= :dateTO")
    List<BizEventEntity> findMBDAttachment(@Param("dateFrom") Long dateFrom, @Param("dateTo") Long dateTo);

    @Override
    List<BizEventEntity> findAll();

}