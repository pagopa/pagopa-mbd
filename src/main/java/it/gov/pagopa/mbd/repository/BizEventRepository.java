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
    @Query("SELECT c.id, c.partitionKey, c.version, c.receiptId, c.timestamp, c.transferList FROM c JOIN tl in c.transferList WHERE tl.MBDAttachment != null AND tl.fiscalCodePA = @creditorInstitutionCode AND c.timestamp >= @dateFrom AND c.timestamp <= @dateTo")
    List<BizEventEntity> getBizEventsByDateFromAndDateToAndEC(@Param("dateFrom") Long dateFrom,
                                                              @Param("dateTo") Long dateTo,
                                                              @Param("creditorInstitutionCode") String creditorInstitutionCode);

}