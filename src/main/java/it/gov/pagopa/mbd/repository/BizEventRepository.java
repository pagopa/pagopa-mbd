package it.gov.pagopa.mbd.repository;

import com.azure.spring.data.cosmos.repository.CosmosRepository;
import com.azure.spring.data.cosmos.repository.Query;
import it.gov.pagopa.mbd.repository.model.BizEventEntity;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BizEventRepository extends CosmosRepository<BizEventEntity, String> {

//    @Query("SELECT c FROM BizEventEntity c JOIN c.transferList tl WHERE tl.MBDAttachment != null and c.timestamp >= @dateFrom and c.timestamp <= @dateTo")
//    @Query("select * from c join tl in c.transferList where tl.MBDAttachment != null and c.timestamp >= @dateFrom and c.timestamp <= @dateTo")
    @Query("select * from c join tl in c.transferList where tl.MBDAttachment != null and c.timestamp >= 1714407502824")
    List<BizEventEntity> getBizEventsByDateFromAndDateTo(@Param("dateFrom") Long dateFrom, @Param("dateTo") Long dateTo);

    @Override
    List<BizEventEntity> findAll();

}