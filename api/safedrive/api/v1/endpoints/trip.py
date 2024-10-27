from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
from typing import List
from uuid import UUID
import logging

from safedrive.database.db import get_db
from safedrive.schemas.trip import (
    TripCreate,
    TripUpdate,
    TripResponse,
)
from safedrive.crud.trip import trip_crud

# Set up logging
logger = logging.getLogger(__name__)
logger.setLevel(logging.INFO)

router = APIRouter()

@router.post("/trips/", response_model=TripResponse)
def create_trip(
    *,
    db: Session = Depends(get_db),
    trip_in: TripCreate,
) -> TripResponse:
    """
    Create a new trip.

    - **driver_profile_id**: UUID of the driver's profile.
    - **start_time**: Start time of the trip in epoch milliseconds.
    - **other fields**: Additional optional fields.
    """
    try:
        # Validation: Ensure necessary fields are not empty or invalid
        if not trip_in.driver_profile_id or not trip_in.start_time:
            logger.error("Driver Profile ID and start time are required to create a trip.")
            raise HTTPException(status_code=400, detail="Driver Profile ID and start time are required")
        new_trip = trip_crud.create(db=db, obj_in=trip_in)
        logger.info(f"Trip created with ID: {new_trip.id.hex()}")
        return TripResponse.model_validate(new_trip)
    except Exception as e:
        logger.exception("Error creating trip")
        raise HTTPException(status_code=500, detail="Internal Server Error")

@router.get("/trips/{trip_id}", response_model=TripResponse)
def get_trip(
    trip_id: UUID,
    db: Session = Depends(get_db),
) -> TripResponse:
    """
    Retrieve a trip by ID.

    - **trip_id**: The UUID of the trip to retrieve.
    """
    try:
        trip = trip_crud.get(db=db, id=trip_id)
        if not trip:
            logger.warning(f"Trip with ID {trip_id} not found.")
            raise HTTPException(status_code=404, detail="Trip not found")
        logger.info(f"Retrieved trip with ID: {trip_id}")
        return TripResponse.model_validate(trip)
    except HTTPException as e:
        raise e
    except Exception as e:
        logger.exception("Error retrieving trip")
        raise HTTPException(status_code=500, detail="Internal Server Error")

@router.get("/trips/", response_model=List[TripResponse])
def get_all_trips(
    skip: int = 0,
    limit: int = 20,
    db: Session = Depends(get_db),
) -> List[TripResponse]:
    """
    Retrieve all trips with optional pagination.

    - **skip**: Number of records to skip.
    - **limit**: Maximum number of records to retrieve (max 100).
    """
    try:
        if limit > 100:
            logger.error("Limit cannot exceed 100 items.")
            raise HTTPException(status_code=400, detail="Limit cannot exceed 100 items")
        trips = trip_crud.get_all(db=db, skip=skip, limit=limit)
        logger.info(f"Retrieved {len(trips)} trips.")
        return [TripResponse.model_validate(trip) for trip in trips]
    except Exception as e:
        logger.exception("Error retrieving trips")
        raise HTTPException(status_code=500, detail="Internal Server Error")

@router.put("/trips/{trip_id}", response_model=TripResponse)
def update_trip(
    trip_id: UUID,
    *,
    db: Session = Depends(get_db),
    trip_in: TripUpdate,
) -> TripResponse:
    """
    Update an existing trip.

    - **trip_id**: The UUID of the trip to update.
    - **trip_in**: The updated data.
    """
    try:
        trip = trip_crud.get(db=db, id=trip_id)
        if not trip:
            logger.warning(f"Trip with ID {trip_id} not found.")
            raise HTTPException(status_code=404, detail="Trip not found")
        updated_trip = trip_crud.update(db=db, db_obj=trip, obj_in=trip_in)
        logger.info(f"Updated trip with ID: {trip_id}")
        return TripResponse.model_validate(updated_trip)
    except HTTPException as e:
        raise e
    except Exception as e:
        logger.exception("Error updating trip")
        raise HTTPException(status_code=500, detail="Internal Server Error")

@router.delete("/trips/{trip_id}", response_model=TripResponse)
def delete_trip(
    trip_id: UUID,
    db: Session = Depends(get_db),
) -> TripResponse:
    """
    Delete a trip by ID.

    - **trip_id**: The UUID of the trip to delete.
    """
    try:
        trip = trip_crud.get(db=db, id=trip_id)
        if not trip:
            logger.warning(f"Trip with ID {trip_id} not found.")
            raise HTTPException(status_code=404, detail="Trip not found")
        deleted_trip = trip_crud.delete(db=db, id=trip_id)
        logger.info(f"Deleted trip with ID: {trip_id}")
        return TripResponse.model_validate(deleted_trip)
    except HTTPException as e:
        raise e
    except Exception as e:
        logger.exception("Error deleting trip")
        raise HTTPException(status_code=500, detail="Internal Server Error")