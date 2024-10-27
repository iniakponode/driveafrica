from sqlalchemy import Column, DateTime, ForeignKey, Integer, Boolean, BINARY
from sqlalchemy.orm import relationship
from uuid import uuid4, UUID

from safedrive.database.base import Base

def generate_uuid_binary():
    return uuid4().bytes

class Trip(Base):
    """
    Trip Model: Represents a trip taken by a driver.

    Attributes:
    - **id**: The primary key representing the unique identifier for each trip.
    - **driver_profile_id**: Foreign key linking the trip to a specific driver profile.
    - **start_date**: The start date and time of the trip.
    - **end_date**: The end date and time of the trip, if available.
    - **start_time**: Start timestamp for the trip in milliseconds.
    - **end_time**: End timestamp for the trip in milliseconds.
    - **synced**: A flag indicating if the trip data has been synced with a remote database.

    Relationships:
    - **driver_profile**: Many-to-One relationship with DriverProfile.
    - **ai_model_inputs**: One-to-Many relationship with AIModelInput.
    - **raw_sensor_data**: One-to-Many relationship with RawSensorData.
    - **unsafe_behaviour**: One-to-Many relationship with UnsafeBehaviour.
    """

    __tablename__ = "trip"

    id = Column(BINARY(16), primary_key=True, default=generate_uuid_binary)
    driver_profile_id = Column(BINARY(16), ForeignKey('driver_profile.driver_profile_id'), nullable=False)
    start_date = Column(DateTime)
    end_date = Column(DateTime)
    start_time = Column(Integer, nullable=False)
    end_time = Column(Integer)
    synced = Column(Boolean, nullable=False)

    # Relationships
    ai_model_inputs = relationship("AIModelInput", back_populates="trip", cascade="all, delete-orphan")
    driver_profile = relationship("DriverProfile", back_populates="trips")
    raw_sensor_data = relationship("RawSensorData", back_populates="trip")
    unsafe_behaviour = relationship("UnsafeBehaviour", back_populates="trip")

    def __repr__(self):
        return f"<Trip(id={self.id.hex()}, driver_profile_id={self.driver_profile_id.hex()})>"

    @property
    def id_uuid(self) -> UUID:
        """Return the UUID representation of the binary ID."""
        return UUID(bytes=self.id)

    @property
    def driver_profile_id_uuid(self) -> UUID:
        """Return the UUID representation of the binary driver_profile_id."""
        return UUID(bytes=self.driver_profile_id)