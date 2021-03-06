package com.example.lly.dto;

import com.example.lly.util.BaseUtil;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Transient;
import java.io.Serial;
import java.io.Serializable;
import java.sql.Timestamp;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StateExposer implements Serializable {

    @Serial
    @Transient
    private static final long serialVersionUID = BaseUtil.SERIAL_VERSION_UID;
    private Boolean ifOpenSeckill;  //活动是否开启
    private String encodedUrl;   //md5加密值, 校验传过来的加密Url
    private Integer seckillInfoId;    //秒杀活动连接

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    private Timestamp now;
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    private Timestamp start;
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    private Timestamp end;

    @Override
    public String toString() {
        return "StateExposer{" +
                "活动状态=" + ifOpenSeckill +
                ", md5加密='" + encodedUrl + '\'' +
                ", 活动ID=" + seckillInfoId +
                ", 当前时刻=" + now +
                ", 开启时刻=" + start +
                ", 结束时刻=" + end +
                '}';
    }

}
