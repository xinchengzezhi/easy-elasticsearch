package cn.yzw.jc2.common.transfer.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import cn.yzw.jc2.common.transfer.job.DTransferJob;
import lombok.Data;

/**
 * @Description: 配置类
 * @Author: lbl 
 * @Date: 2024/10/17
 **/
@Data
@Configuration
public class DTransferConfig {

    @Bean("retryTaskJob")
    public DTransferJob RetryTaskJob() {
        return new DTransferJob();
    }

}