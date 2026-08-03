ALTER TABLE food_demand
  ADD COLUMN comparison_selected_demand_item_ids_json JSON NULL AFTER traffic_service_json;
