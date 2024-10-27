from sqlalchemy import Column, Float, UUID, DateTime, Boolean, String
from sqlalchemy.orm import relationship
from sqlalchemy.ext.declarative import declarative_base
from datetime import datetime
import uuid
from safedrive.database.base import Base

def generate_uuid():
        return str(uuid.uuid4())  # Generates a UUID string

class Location(Base):
    """
    Location model representing the geographical information captured during a trip.

    Attributes:
    - id (UUID): Unique identifier for each location.
    - latitude (float): Latitude of the recorded location.
    - longitude (float): Longitude of the recorded location.
    - timestamp (datetime): The exact time when the location was recorded.
    - date (datetime): The date the location was recorded.
    - altitude (float): Altitude of the recorded location.
    - speed (float): Speed of the vehicle at the recorded location.
    - distance (float): The distance covered from the previous recorded point.
    - sync (bool): Status to indicate whether the data has been synced to the server.
    """
    __tablename__ = 'location'
    

    id = Column(String(36), primary_key=True, default=generate_uuid)
    latitude = Column(Float, nullable=False)
    longitude = Column(Float, nullable=False)
    timestamp = Column(DateTime, nullable=False)
    date = Column(DateTime, nullable=False)
    altitude = Column(Float, nullable=False)
    speed = Column(Float, nullable=False)
    distance = Column(Float, nullable=False)
    sync = Column(Boolean, nullable=False)

    # Relationships
    raw_sensor_data = relationship("RawSensorData", back_populates="location", cascade="all, delete-orphan")
    unsafe_behaviour = relationship("UnsafeBehaviour", back_populates="location", cascade="all, delete-orphan")

    def __repr__(self):
        return (
            f"<Location(id={self.id}, latitude={self.latitude}, longitude={self.longitude}, "
            f"timestamp={self.timestamp}, date={self.date}, altitude={self.altitude}, "
            f"speed={self.speed}, distance={self.distance}, sync={self.sync})>"
        )