from pydantic import BaseModel
from typing import Optional
from uuid import UUID
from datetime import datetime

class CauseBase(BaseModel):
    """
    Base schema for Cause entity representing attributes that are common across different operations.

    Attributes:
        id (UUID): The unique identifier for the cause.
        unsafeBehaviourId (UUID): The identifier for the related unsafe behaviour.
        name (str): The name of the cause.
        influence (Optional[bool]): Indicates whether the cause has influence. Defaults to None.
        createdAt (datetime): The timestamp when the cause was created.
        updatedAt (Optional[datetime]): The timestamp when the cause was last updated. Defaults to None.
    """
    id: UUID
    unsafeBehaviourId: UUID
    name: str
    influence: Optional[bool] = None
    createdAt: datetime
    updatedAt: Optional[datetime] = None

    class Config:
        orm_mode = True

class CauseCreate(CauseBase):
    """
    Schema for creating a new Cause entity.
    Inherits all attributes from CauseBase.
    """
    pass

class CauseUpdate(BaseModel):
    """
    Schema for updating an existing Cause entity.
    Attributes are optional to allow partial updates.

    Attributes:
        unsafeBehaviourId (Optional[UUID]): The identifier for the related unsafe behaviour.
        name (Optional[str]): The name of the cause.
        influence (Optional[bool]): Indicates whether the cause has influence.
        updatedAt (Optional[datetime]): The timestamp when the cause was last updated.
    """
    unsafeBehaviourId: Optional[UUID] = None
    name: Optional[str] = None
    influence: Optional[bool] = None
    updatedAt: Optional[datetime] = None

    class Config:
        orm_mode = True

class CauseResponse(CauseBase):
    """
    Schema for response when retrieving a Cause entity.
    Inherits all attributes from CauseBase.
    """
    pass