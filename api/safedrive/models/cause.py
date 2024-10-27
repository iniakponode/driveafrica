from uuid import uuid4
from sqlalchemy import BINARY, Column, String, Boolean, ForeignKey, DateTime, UUID
from sqlalchemy.orm import relationship
from sqlalchemy.ext.declarative import declarative_base
from safedrive.database.base import Base
from datetime import datetime

def generate_uuid():
        return str(uuid4())  # Generates a UUID string

class Cause(Base):
    """
    Cause is the SQLAlchemy ORM model class representing the 'causes' table.

    Attributes:
    - id (UUID): Primary key representing the unique identifier for each cause.
    - unsafe_behaviour_id (UUID): Foreign key linking to the UnsafeBehaviour entity.
    - name (str): The name of the cause (e.g., "Alcohol Influence", "Distracted Driving").
    - influence (bool): Indicates whether this cause has an influence on the unsafe behavior.
    - created_at (datetime): The timestamp when this cause was first recorded.
    - updated_at (Optional[datetime]): The timestamp for the last update to this cause (if any).
    - unsafe_behaviour (Relationship): Establishes a relationship to the UnsafeBehaviour entity.
    """

    __tablename__ = "causes"

    id = Column(String(36), primary_key=True, default=generate_uuid)
    unsafe_behaviour_id = Column(BINARY(16), ForeignKey('unsafe_behaviour.id', ondelete="CASCADE"), nullable=False)
    name = Column(String(255), nullable=False)
    influence = Column(Boolean)
    created_at = Column(DateTime, nullable=False)
    updated_at = Column(DateTime)

    # Relationship with UnsafeBehaviour
    unsafe_behaviour = relationship("UnsafeBehaviour", back_populates="causes")

    def __repr__(self):
        return f"<Cause(id={self.id}, unsafe_behaviour_id={self.unsafe_behaviour_id}, name={self.name})>"
