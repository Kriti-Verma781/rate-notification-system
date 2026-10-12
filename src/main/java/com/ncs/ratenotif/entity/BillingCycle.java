package com.ncs.ratenotif.entity;

public enum BillingCycle {

	ONE_BY_ONE(1, 1), SIXTY_BY_ONE(60, 1), SIXTY_BY_SIXTY(60, 60), THIRTY_BY_ONE(30, 1), THIRTY_BY_SIX(30, 6),
	FIFTEEN_BY_FIFTEEN(15, 15), SIX_BY_SIX(6, 6);

	private final int billingDays;
	private final int payableDays;

	BillingCycle(int billingDays, int payableDays) {
		this.billingDays = billingDays;
		this.payableDays = payableDays;
	}

	public String getLabel() {
		return billingDays + "/" + payableDays;
	}
}
