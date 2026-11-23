package com.sanika.bicyclebookingapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.sanika.bicyclebookingapp.model.SupportTicket;
import com.sanika.bicyclebookingapp.model.TicketMessage;

import org.junit.Test;

import java.util.Date;
import java.util.Locale;

/**
 * Unit tests for Phase 9: Customer Support and Complaint Management.
 * Validates ticket entity creation, all 7 lifecycle status transitions,
 * category classifications, threaded conversation message modeling,
 * disputed amount precision, resolution recording, and customer reopening rules.
 */
public class Phase9SupportComplaintTest {

    @Test
    public void testTicketCreationAndDefaults() {
        SupportTicket ticket = new SupportTicket();
        ticket.setTicketId("TKT-TEST-001");
        ticket.setUserId("CUST-101");
        ticket.setUserName("Aarav Mehta");
        ticket.setUserPhone("+91 98765 12345");
        ticket.setSubject("UPI Payment deducted but cycle did not unlock");
        ticket.setDescription("Paid ₹50 via Google Pay UPI. Station lock remained jammed.");
        ticket.setCategory(SupportTicket.CATEGORY_PAYMENT_PROBLEM);
        ticket.setPriority(SupportTicket.PRIORITY_URGENT);
        ticket.setStatus(SupportTicket.STATUS_OPEN);
        ticket.setBookingId("BK-501");
        ticket.setBicycleId("BIK-12");
        ticket.setDisputedAmountPaise(5000); // ₹50.00
        ticket.setCreatedAt(new Date());

        assertEquals("TKT-TEST-001", ticket.getTicketId());
        assertEquals("CUST-101", ticket.getUserId());
        assertEquals(SupportTicket.CATEGORY_PAYMENT_PROBLEM, ticket.getCategory());
        assertEquals(SupportTicket.PRIORITY_URGENT, ticket.getPriority());
        assertEquals(SupportTicket.STATUS_OPEN, ticket.getStatus());
        assertEquals("BK-501", ticket.getBookingId());
        assertEquals(5000L, ticket.getDisputedAmountPaise());
        assertEquals("50.00", String.format(Locale.ENGLISH, "%.2f", ticket.getDisputedAmountPaise() / 100.0));
    }

    @Test
    public void testAllSevenStatusTransitions() {
        SupportTicket ticket = new SupportTicket();
        ticket.setStatus(SupportTicket.STATUS_OPEN);

        // OPEN -> ASSIGNED
        assertTrue("OPEN can transition to ASSIGNED",
                ticket.isValidTransition(SupportTicket.STATUS_ASSIGNED));
        // OPEN -> IN_PROGRESS
        assertTrue("OPEN can transition to IN_PROGRESS",
                ticket.isValidTransition(SupportTicket.STATUS_IN_PROGRESS));
        // OPEN -> RESOLVED
        assertTrue("OPEN can transition directly to RESOLVED",
                ticket.isValidTransition(SupportTicket.STATUS_RESOLVED));

        // ASSIGNED -> IN_PROGRESS
        ticket.setStatus(SupportTicket.STATUS_ASSIGNED);
        assertTrue("ASSIGNED can transition to IN_PROGRESS",
                ticket.isValidTransition(SupportTicket.STATUS_IN_PROGRESS));

        // IN_PROGRESS -> WAITING_FOR_CUSTOMER
        ticket.setStatus(SupportTicket.STATUS_IN_PROGRESS);
        assertTrue("IN_PROGRESS can transition to WAITING_FOR_CUSTOMER",
                ticket.isValidTransition(SupportTicket.STATUS_WAITING_FOR_CUSTOMER));
        assertTrue("IN_PROGRESS can transition to RESOLVED",
                ticket.isValidTransition(SupportTicket.STATUS_RESOLVED));

        // WAITING_FOR_CUSTOMER -> IN_PROGRESS (Customer replies)
        ticket.setStatus(SupportTicket.STATUS_WAITING_FOR_CUSTOMER);
        assertTrue("WAITING_FOR_CUSTOMER can transition to IN_PROGRESS",
                ticket.isValidTransition(SupportTicket.STATUS_IN_PROGRESS));
        assertTrue("WAITING_FOR_CUSTOMER can transition to RESOLVED",
                ticket.isValidTransition(SupportTicket.STATUS_RESOLVED));

        // RESOLVED -> REOPENED (Customer reopens)
        ticket.setStatus(SupportTicket.STATUS_RESOLVED);
        assertTrue("RESOLVED can be REOPENED",
                ticket.isValidTransition(SupportTicket.STATUS_REOPENED));
        assertTrue("RESOLVED can be CLOSED",
                ticket.isValidTransition(SupportTicket.STATUS_CLOSED));

        // CLOSED -> REOPENED
        ticket.setStatus(SupportTicket.STATUS_CLOSED);
        assertTrue("CLOSED ticket can be REOPENED",
                ticket.isValidTransition(SupportTicket.STATUS_REOPENED));
        assertFalse("CLOSED cannot directly transition to ASSIGNED without reopening",
                ticket.isValidTransition(SupportTicket.STATUS_ASSIGNED));

        // REOPENED -> IN_PROGRESS / ASSIGNED
        ticket.setStatus(SupportTicket.STATUS_REOPENED);
        assertTrue("REOPENED can transition to IN_PROGRESS",
                ticket.isValidTransition(SupportTicket.STATUS_IN_PROGRESS));
        assertTrue("REOPENED can transition to ASSIGNED",
                ticket.isValidTransition(SupportTicket.STATUS_ASSIGNED));
    }

    @Test
    public void testThreadedMessageModeling() {
        TicketMessage customerMsg = new TicketMessage(
                "TKT-001",
                "CUST-101",
                "Aarav Mehta",
                TicketMessage.SENDER_CUSTOMER,
                "Here is the screenshot of the UPI transaction failure."
        );
        customerMsg.setAttachmentUrl("content://media/external/images/media/42");

        assertEquals("TKT-001", customerMsg.getTicketId());
        assertEquals("CUST-101", customerMsg.getSenderId());
        assertEquals("Aarav Mehta", customerMsg.getSenderName());
        assertEquals(TicketMessage.SENDER_CUSTOMER, customerMsg.getSenderRole());
        assertNotNull(customerMsg.getCreatedAt());
        assertEquals("content://media/external/images/media/42", customerMsg.getAttachmentUrl());

        TicketMessage supportReply = new TicketMessage(
                "TKT-001",
                "AGENT-09",
                "Pooja Deshmukh",
                TicketMessage.SENDER_SUPPORT,
                "We verified the transaction. A refund of ₹50 has been processed to your bank."
        );
        assertEquals(TicketMessage.SENDER_SUPPORT, supportReply.getSenderRole());
    }

    @Test
    public void testResolutionAndAdminClosure() {
        SupportTicket ticket = new SupportTicket();
        ticket.setTicketId("TKT-002");
        ticket.setStatus(SupportTicket.STATUS_IN_PROGRESS);

        // Support agent resolves ticket
        String resolution = "Refund of ₹70 initiated via Razorpay ARN 123456789. Dock B-02 unlocked.";
        ticket.setStatus(SupportTicket.STATUS_RESOLVED);
        ticket.setResolutionSummary(resolution);
        ticket.setAdminNotes(resolution);
        ticket.setResolvedBy("ADMIN_01");
        ticket.setResolvedAt(new Date());

        assertEquals(SupportTicket.STATUS_RESOLVED, ticket.getStatus());
        assertEquals(resolution, ticket.getResolutionSummary());
        assertEquals(resolution, ticket.getAdminNotes());
        assertEquals("ADMIN_01", ticket.getResolvedBy());
        assertNotNull(ticket.getResolvedAt());

        // Subsequent closure
        ticket.setStatus(SupportTicket.STATUS_CLOSED);
        ticket.setClosedAt(new Date());
        assertEquals(SupportTicket.STATUS_CLOSED, ticket.getStatus());
        assertNotNull(ticket.getClosedAt());
    }

    @Test
    public void testReopeningWorkflow() {
        SupportTicket ticket = new SupportTicket();
        ticket.setTicketId("TKT-003");
        ticket.setStatus(SupportTicket.STATUS_RESOLVED);
        ticket.setResolutionSummary("Marked resolved by auto-bot");

        // Customer reopens ticket
        assertTrue("Customer must be permitted to reopen resolved grievance",
                ticket.isValidTransition(SupportTicket.STATUS_REOPENED));

        ticket.setStatus(SupportTicket.STATUS_REOPENED);
        ticket.setReopenedAt(new Date());

        assertEquals(SupportTicket.STATUS_REOPENED, ticket.getStatus());
        assertNotNull(ticket.getReopenedAt());
    }

    @Test
    public void testCategoryClassifications() {
        assertEquals("BILLING_DISPUTE", SupportTicket.CATEGORY_BILLING_DISPUTE);
        assertEquals("RIDE_PROBLEM", SupportTicket.CATEGORY_RIDE_PROBLEM);
        assertEquals("PAYMENT_PROBLEM", SupportTicket.CATEGORY_PAYMENT_PROBLEM);
        assertEquals("DAMAGED_BICYCLE", SupportTicket.CATEGORY_DAMAGED_BICYCLE);
        assertEquals("UNSAFE_BICYCLE", SupportTicket.CATEGORY_UNSAFE_BICYCLE);
        assertEquals("REFUND_REQUEST", SupportTicket.CATEGORY_REFUND_REQUEST);
        assertEquals("STATION_PROBLEM", SupportTicket.CATEGORY_STATION_PROBLEM);
        assertEquals("ACCOUNT_LOGIN", SupportTicket.CATEGORY_ACCOUNT_LOGIN);
        assertEquals("GENERAL", SupportTicket.CATEGORY_GENERAL);
    }
}
