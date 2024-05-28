package it.gov.pagopa.mbd.repository;

import com.azure.spring.data.cosmos.repository.CosmosRepository;
import com.azure.spring.data.cosmos.repository.Query;
import it.gov.pagopa.mbd.repository.model.BizEventEntity;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BizEventRepository extends CosmosRepository<BizEventEntity, String> {

    @Query("SELECT * FROM BizEventEntity c WHERE JOIN tl IN c.transferList WHERE tl.MBDAttachment != null")
    List<BizEventEntity> findMBDAttachment();
}