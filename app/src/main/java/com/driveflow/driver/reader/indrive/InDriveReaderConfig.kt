package com.driveflow.driver.reader.indrive

import kotlinx.serialization.Serializable

@Serializable
data class InDriveReaderConfig(
    val packageName: String = "sinet.startup.inDriver",
    val revision: Int = 1,
    val listContainerIds: List<String> = listOf(
        "sinet.startup.inDriver:id/rv_orders",
        "sinet.startup.inDriver:id/recycler_view",
        "sinet.startup.inDriver:id/orders_list",
        "sinet.startup.inDriver:id/orders_container",
        "sinet.startup.inDriver:id/order_list",
        "rv_orders",
        "recycler_view",
        "orders_list"
    ),
    val itemCardIds: List<String> = listOf(
        "sinet.startup.inDriver:id/order_item",
        "sinet.startup.inDriver:id/item_order",
        "sinet.startup.inDriver:id/order_card",
        "sinet.startup.inDriver:id/card_view",
        "sinet.startup.inDriver:id/order_item_root",
        "order_item",
        "item_order",
        "order_card"
    ),
    val fareViewIds: List<String> = listOf(
        "sinet.startup.inDriver:id/tv_cost",
        "sinet.startup.inDriver:id/tv_price",
        "sinet.startup.inDriver:id/order_cost",
        "sinet.startup.inDriver:id/tv_order_cost",
        "sinet.startup.inDriver:id/cost",
        "sinet.startup.inDriver:id/price_text",
        "tv_cost",
        "tv_price",
        "order_cost"
    ),
    val pickupDistanceIds: List<String> = listOf(
        "sinet.startup.inDriver:id/tv_pickup_distance",
        "sinet.startup.inDriver:id/tv_distance",
        "sinet.startup.inDriver:id/pickup_distance",
        "sinet.startup.inDriver:id/tv_distance_to_pickup",
        "tv_pickup_distance",
        "tv_distance"
    ),
    val tripDistanceIds: List<String> = listOf(
        "sinet.startup.inDriver:id/tv_trip_distance",
        "sinet.startup.inDriver:id/tv_route_distance",
        "sinet.startup.inDriver:id/trip_distance",
        "sinet.startup.inDriver:id/tv_total_distance",
        "tv_trip_distance",
        "tv_route_distance"
    ),
    val pickupAddressIds: List<String> = listOf(
        "sinet.startup.inDriver:id/tv_source_address",
        "sinet.startup.inDriver:id/tv_pickup_address",
        "sinet.startup.inDriver:id/tv_from_address",
        "sinet.startup.inDriver:id/tv_address_from",
        "sinet.startup.inDriver:id/source_address",
        "tv_source_address",
        "tv_pickup_address",
        "tv_from_address"
    ),
    val destinationAddressIds: List<String> = listOf(
        "sinet.startup.inDriver:id/tv_destination_address",
        "sinet.startup.inDriver:id/tv_dest_address",
        "sinet.startup.inDriver:id/tv_to_address",
        "sinet.startup.inDriver:id/tv_address_to",
        "sinet.startup.inDriver:id/dest_address",
        "tv_destination_address",
        "tv_dest_address",
        "tv_to_address"
    ),
    val extraStopsIds: List<String> = listOf(
        "sinet.startup.inDriver:id/tv_stops",
        "sinet.startup.inDriver:id/tv_extra_stops",
        "sinet.startup.inDriver:id/layout_stops",
        "sinet.startup.inDriver:id/order_stops",
        "tv_stops",
        "layout_stops"
    ),
    val ratingIds: List<String> = listOf(
        "sinet.startup.inDriver:id/tv_rating",
        "sinet.startup.inDriver:id/tv_user_rating",
        "sinet.startup.inDriver:id/user_rating",
        "sinet.startup.inDriver:id/rating_bar",
        "tv_rating",
        "user_rating"
    ),
    val tripsCountIds: List<String> = listOf(
        "sinet.startup.inDriver:id/tv_trips_count",
        "sinet.startup.inDriver:id/tv_rating_count",
        "sinet.startup.inDriver:id/tv_trips",
        "sinet.startup.inDriver:id/trips_count",
        "tv_trips_count",
        "tv_trips"
    ),
    val requestAgeIds: List<String> = listOf(
        "sinet.startup.inDriver:id/tv_time",
        "sinet.startup.inDriver:id/tv_age",
        "sinet.startup.inDriver:id/tv_created_at",
        "sinet.startup.inDriver:id/order_time",
        "tv_time",
        "tv_age"
    ),
    val paymentMethodIds: List<String> = listOf(
        "sinet.startup.inDriver:id/tv_payment_method",
        "sinet.startup.inDriver:id/tv_payment_type",
        "sinet.startup.inDriver:id/tv_payment",
        "sinet.startup.inDriver:id/payment_type",
        "tv_payment_method",
        "tv_payment"
    ),
    val riderNoteIds: List<String> = listOf(
        "sinet.startup.inDriver:id/tv_comment",
        "sinet.startup.inDriver:id/tv_note",
        "sinet.startup.inDriver:id/tv_order_comment",
        "sinet.startup.inDriver:id/order_comment",
        "tv_comment",
        "tv_note"
    ),
    val offersCountIds: List<String> = listOf(
        "sinet.startup.inDriver:id/tv_offers",
        "sinet.startup.inDriver:id/tv_offers_count",
        "sinet.startup.inDriver:id/offers_count",
        "tv_offers"
    ),
    val roadStateIds: List<String> = listOf(
        "sinet.startup.inDriver:id/tv_traffic",
        "sinet.startup.inDriver:id/tv_road_state",
        "tv_traffic"
    ),
    val detailContainerIds: List<String> = listOf(
        "sinet.startup.inDriver:id/order_details",
        "sinet.startup.inDriver:id/layout_order_detail",
        "sinet.startup.inDriver:id/bottom_sheet_order",
        "sinet.startup.inDriver:id/order_detail_root",
        "sinet.startup.inDriver:id/dialog_order",
        "order_details",
        "bottom_sheet_order"
    ),
    val mapContainerIds: List<String> = listOf(
        "sinet.startup.inDriver:id/map_container",
        "sinet.startup.inDriver:id/map_view",
        "sinet.startup.inDriver:id/map",
        "sinet.startup.inDriver:id/mapView",
        "map_container",
        "map_view"
    )
) {
    companion object {
        val DEFAULT = InDriveReaderConfig()
    }
}
