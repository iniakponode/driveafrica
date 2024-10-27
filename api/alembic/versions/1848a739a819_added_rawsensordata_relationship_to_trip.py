"""added rawsensordata relationship to trip

Revision ID: 1848a739a819
Revises: 5acd9d4a493c
Create Date: 2024-10-14 17:26:37.848150

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision: str = '1848a739a819'
down_revision: Union[str, None] = '5acd9d4a493c'
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    pass


def downgrade() -> None:
    pass
