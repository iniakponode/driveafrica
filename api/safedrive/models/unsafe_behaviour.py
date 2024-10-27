from sqlalchemy.dialects.mysql import BINARY
from sqlalchemy import Column, Float, String, DateTime, Boolean, ForeignKey
from sqlalchemy.dialects.mysql import CHAR
from sqlalchemy.orm import relationship
from safedrive.database.base import Base
import uuid
from datetime import datetime
from sqlalchemy_utils import UUIDType


def generate_uuid():
        return str(uuid.uuid4())  # Generates a UUID string

class UnsafeBehaviour(Base):
    """
    Represents the unsafe_behaviour table in the database.

    Attributes:
    - id (CHAR): Primary key representing the unique ID of the unsafe behaviour.
    - trip_id (CHAR): Foreign key to Trip model representing the trip associated with the unsafe behaviour.
    - location_id (CHAR): Foreign key to Location model representing the location associated with the behaviour.
    - behavior_type (String): Type of the unsafe behaviour detected.
    - severity (Float): Severity level of the unsafe behaviour.
    - timestamp (int): The Unix timestamp representing the time when the behaviour was detected.
    - date (datetime): The date when the behaviour occurred.
    - updated_at (datetime): The timestamp when the record was last updated.
    - updated (Boolean): Flag indicating if the behaviour record was updated.
    - synced (Boolean): Flag indicating if the record has been synced with external storage.
    - alcohol_influence (Boolean): Flag indicating if the behaviour was influenced by alcohol.
    
    Relationships:
    - trip: Relationship with the Trip model.
    - location: Relationship with the Location model.
    """

    __tablename__ = "unsafe_behaviour"
    
    id = Column(BINARY(16), primary_key=True, unique=True, default=generate_uuid)
    tripId = Column(BINARY(16), ForeignKey('trip_data.id'), nullable=False)
    location_id = Column(String(36), ForeignKey('location.id'))
    behavior_type = Column(String(255), nullable=False)
    severity = Column(Float, nullable=False)
    timestamp = Column(String(255), nullable=False)
    date = Column(DateTime)
    updated_at = Column(DateTime)
    updated = Column(Boolean)
    synced = Column(Boolean)
    alcohol_influence = Column(Boolean)

    # Relationships
    location = relationship("Location", back_populates="unsafe_behaviour")
    trip_data = relationship("Trip", back_populates="unsafe_behaviour")
    causes = relationship("Cause", back_populates="unsafe_behaviour", cascade="all, delete-orphan")
    

    def __repr__(self):
        return f"<UnsafeBehaviour(id='{self.id}', behavior_type='{self.behavior_type}', severity='{self.severity}')>"

    def to_dict(self):
        return {
            "id": self.id,
            "trip_id": self.trip_id,
            "location_id": self.location_id,
            "behavior_type": self.behavior_type,
            "severity": self.severity,
            "timestamp": self.timestamp,
            "date": self.date.isoformat() if self.date else None,
            "updated_at": self.updated_at.isoformat() if self.updated_at else None,
            "updated": self.updated,
            "synced": self.synced,
            "alcohol_influence": self.alcohol_influence,
        }