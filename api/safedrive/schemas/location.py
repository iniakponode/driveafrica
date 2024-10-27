from pydantic import BaseModel
from typing import Optional
from uuid import UUID
from datetime import datetime

class LocationBase(BaseModel):
    """
    Base class for the Location model, defining common attributes.

    Attributes:
    - id (UUID): The unique identifier for each location entry.
    - latitude (float): The latitude coordinate of the location.
    - longitude (float): The longitude coordinate of the location.
    - timestamp (int): The timestamp indicating when the location data was recorded.
    - date (datetime): The date corresponding to the location data.
    - altitude (float): The altitude of the location in meters.
    - speed (float): The speed of the vehicle/person at the given location.
    - distance (float): The distance traveled from the previous location.
    - sync (bool): Indicator whether the data has been synced.
    """
    id: UUID
    latitude: float
    longitude: float
    timestamp: int
    date: datetime
    altitude: float
    speed: float
    distance: float
    sync: bool

    class Config:
        orm_mode = True

class LocationCreate(LocationBase):
    """
    Schema for creating a new Location record.

    Inherits all attributes from LocationBase.
    """
    pass

class LocationUpdate(BaseModel):
    """
    Schema for updating an existing Location record.

    Attributes:
    - latitude (Optional[float]): Optionally update the latitude of the location.
    - longitude (Optional[float]): Optionally update the longitude of the location.
    - timestamp (Optional[int]): Optionally update the timestamp of the location.
    - date (Optional[datetime]): Optionally update the date of the location.
    - altitude (Optional[float]): Optionally update the altitude.
    - speed (Optional[float]): Optionally update the speed at the given location.
    - distance (Optional[float]): Optionally update the distance traveled from the previous location.
    - sync (Optional[bool]): Optionally update the sync status.
    """
    latitude: Optional[float] = None
    longitude: Optional[float] = None
    timestamp: Optional[int] = None
    date: Optional[datetime] = None
    altitude: Optional[float] = None
    speed: Optional[float] = None
    distance: Optional[float] = None
    sync: Optional[bool] = None

    class Config:
        orm_mode = True

class LocationResponse(LocationBase):
    """
    Schema for the response format of a Location record.

    Inherits all attributes from LocationBase.
    """
    pass