package org.example.ingresso.ingresso.service;

import org.example.ingresso.ingresso.service.dto.TicketDeliveryContracts.TicketView;

import java.util.List;

public interface TicketDeliveryService {

    void issueForPaidOrder(Long orderId);

    List<TicketView> ticketsForOrder(Long orderId);
}