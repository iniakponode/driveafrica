from pydantic import BaseModel
from typing import Optional
from uuid import UUID
from datetime import date

class DrivingTipBase(BaseModel):
    """
    Base schema for Driving Tip entity representing attributes that are common across different operations.

    Attributes:
        tipId (UUID): The unique identifier for the driving tip.
        title (str): The title of the driving tip.
        meaning (Optional[str]): Explanation of the behavior. Defaults to None.
        penalty (Optional[str]): Applicable penalties in a neutral tone. Defaults to None.
        fine (Optional[str]): Applicable fines in a neutral tone. Defaults to None.
        law (Optional[str]): Relevant laws stated factually. Defaults to None.
        hostility (Optional[str]): Indicates any hostile action or behavior. Defaults to None.
        summaryTip (Optional[str]): Encouraging, actionable advice. Defaults to None.
        sync (bool): Indicates whether the tip is synced. Defaults to False.
        date (date): The date the driving tip was created.
        profileId (UUID): The identifier of the driver profile associated with the tip.
        llm (Optional[str]): Indicates the language model used for generating the tip. Defaults to None.
    """
    tipId: UUID
    title: str
    meaning: Optional[str] = None
    penalty: Optional[str] = None
    fine: Optional[str] = None
    law: Optional[str] = None
    hostility: Optional[str] = None
    summaryTip: Optional[str] = None
    sync: bool = False
    date: date
    profileId: UUID
    llm: Optional[str] = None

    class Config:
        orm_mode = True

class DrivingTipCreate(DrivingTipBase):
    """
    Schema for creating a new Driving Tip entity.
    Inherits all attributes from DrivingTipBase.
    """
    pass

class DrivingTipUpdate(BaseModel):
    """
    Schema for updating an existing Driving Tip entity.
    Attributes are optional to allow partial updates.

    Attributes:
        title (Optional[str]): The title of the driving tip.
        meaning (Optional[str]): Explanation of the behavior.
        penalty (Optional[str]): Applicable penalties in a neutral tone.
        fine (Optional[str]): Applicable fines in a neutral tone.
        law (Optional[str]): Relevant laws stated factually.
        hostility (Optional[str]): Indicates any hostile action or behavior.
        summaryTip (Optional[str]): Encouraging, actionable advice.
        sync (Optional[bool]): Indicates whether the tip is synced.
        date (Optional[date]): The date the driving tip was created.
        profileId (Optional[UUID]): The identifier of the driver profile associated with the tip.
        llm (Optional[str]): Indicates the language model used for generating the tip.
    """
    title: Optional[str] = None
    meaning: Optional[str] = None
    penalty: Optional[str] = None
    fine: Optional[str] = None
    law: Optional[str] = None
    hostility: Optional[str] = None
    summaryTip: Optional[str] = None
    sync: Optional[bool] = None
    date: Optional[date] = None
    profileId: Optional[UUID] = None
    llm: Optional[str] = None

    class Config:
        orm_mode = True

class DrivingTipResponse(DrivingTipBase):
    """
    Schema for response when retrieving a Driving Tip entity.
    Inherits all attributes from DrivingTipBase.
    """
    pass