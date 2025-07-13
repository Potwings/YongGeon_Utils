package org.potwings.utils.menufinder.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class StoreInfo {
    private String storeName;
    private List<String> menuList;
    private String storeURL;
}
