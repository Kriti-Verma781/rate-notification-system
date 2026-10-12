package com.ncs.ratenotif.request;

import java.util.List;

import lombok.Data;

@Data
public class CountryRateUpdate {

    private List<NetworkRateUpdate> networks;
}
